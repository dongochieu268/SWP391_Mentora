package com.edunac.mentora.service.learning;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import com.edunac.mentora.domain.classroom.NodeVisibilityStatus;
import com.edunac.mentora.domain.learning.NodeProgress;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.dto.NodeProgressResponse;
import com.edunac.mentora.repository.classroom.ClassroomMemberRepository;
import com.edunac.mentora.repository.classroom.ClassroomNodeStatusRepository;
import com.edunac.mentora.repository.learning.NodeProgressRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NodeProgressService {

    private final NodeProgressRepository nodeProgressRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final ClassroomNodeStatusRepository classroomNodeStatusRepository;


    public NodeProgressResponse markNodeCompleted(
            Integer studentId, Integer nodeId, Integer classroomId, BigDecimal score) {

        ensureActiveMember(studentId, classroomId);
        ensureNodeVisible(nodeId, classroomId);

        LearningNode node = learningNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Node không tồn tại"));

        if (node.getPrerequisite() != null) {
            boolean prereqDone = nodeProgressRepository
                    .existsByStudent_IdAndLearningNode_IdAndClassroom_IdAndCompletedTrue(
                            studentId, node.getPrerequisite().getId(), classroomId);
            if (!prereqDone) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn cần hoàn thành node tiên quyết trước: "
                                + node.getPrerequisite().getTitle());
            }
        }

        saveCompleted(studentId, nodeId, classroomId, score);
        return buildProgressResponse(studentId, classroomId, nodeId);
    }


    void saveCompleted(Integer studentId, Integer nodeId, Integer classroomId, BigDecimal score) {
        NodeProgress progress = nodeProgressRepository
                .findByStudent_IdAndLearningNode_IdAndClassroom_Id(
                        studentId, nodeId, classroomId)
                .orElseGet(() -> {
                    NodeProgress np = new NodeProgress();
                    User s = new User(); s.setId(studentId);
                    np.setStudent(s);
                    LearningNode n = new LearningNode(); n.setId(nodeId);
                    np.setLearningNode(n);
                    Classroom c = new Classroom(); c.setId(classroomId);
                    np.setClassroom(c);
                    np.setCompleted(false);
                    return np;
                });

        boolean changed = false;

        if (!progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            changed = true;
        }

        if (score != null
                && (progress.getBestScore() == null || score.compareTo(progress.getBestScore()) > 0)) {
            progress.setBestScore(score);
            changed = true;
        }

        if (changed) {
            nodeProgressRepository.save(progress);
        }
    }


    @Transactional(readOnly = true)
    public NodeProgressResponse buildProgressResponse(
            Integer studentId, Integer classroomId, Integer nodeId) {

        ensureActiveMember(studentId, classroomId);


        long totalNodes     = nodeProgressRepository
                .countRelevantNodesForStudent(classroomId);
        long completedNodes = nodeProgressRepository
                .countCompletedNodesForStudent(studentId, classroomId);

        completedNodes = Math.min(completedNodes, totalNodes);

        double percent = totalNodes == 0 ? 0.0
                : Math.round(completedNodes * 100.0 / totalNodes * 10.0) / 10.0;

        LearningNode currentNode = learningNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Node không tồn tại"));

        NodeProgress progress = nodeProgressRepository
                .findByStudent_IdAndLearningNode_IdAndClassroom_Id(studentId, nodeId, classroomId)
                .orElse(null);
        boolean isCompleted = progress != null && progress.isCompleted();

        boolean prerequisiteMet = true;
        if (currentNode.getPrerequisite() != null) {
            prerequisiteMet = nodeProgressRepository
                    .existsByStudent_IdAndLearningNode_IdAndClassroom_IdAndCompletedTrue(
                            studentId, currentNode.getPrerequisite().getId(), classroomId);
        }

        return NodeProgressResponse.builder()
                .nodeId(nodeId)
                .nodeTitle(currentNode.getTitle())
                .completed(isCompleted)
                .totalNodes((int) totalNodes)
                .completedNodes((int) completedNodes)
                .progressPercent(percent)
                .prerequisiteMet(prerequisiteMet)
                .bestScore(progress != null ? progress.getBestScore() : null)
                .bestLevelNumber(progress != null ? progress.getBestLevelNumber() : null)
                .build();
    }

    @Transactional(readOnly = true)
    public List<NodeProgress> getProgressByStudentAndClassroom(
            Integer studentId, Integer classroomId) {
        return nodeProgressRepository.findByStudent_IdAndClassroom_Id(studentId, classroomId);
    }


    public void updateOnAttemptSubmitted(Integer nodeId, Integer studentId, Integer classroomId,
                                         Integer levelNumber, BigDecimal score) {
        NodeProgress progress = nodeProgressRepository
                .findByStudent_IdAndLearningNode_IdAndClassroom_Id(studentId, nodeId, classroomId)
                .orElseGet(() -> {
                    NodeProgress np = new NodeProgress();
                    User s = new User(); s.setId(studentId);
                    np.setStudent(s);
                    LearningNode n = new LearningNode(); n.setId(nodeId);
                    np.setLearningNode(n);
                    Classroom c = new Classroom(); c.setId(classroomId);
                    np.setClassroom(c);
                    np.setCompleted(false);
                    return np;
                });

        if (!progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }

        if (progress.getBestScore() == null || score.compareTo(progress.getBestScore()) > 0) {
            progress.setBestScore(score);
            progress.setBestLevelNumber(levelNumber);
        }

        nodeProgressRepository.save(progress);
    }


    private void ensureActiveMember(Integer studentId, Integer classroomId) {
        boolean isMember = classroomMemberRepository
                .existsByClassroomIdAndUserIdAndStatus(classroomId, studentId, "ACTIVE");
        if (!isMember) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Bạn không phải thành viên của lớp này");
        }
    }

    private void ensureNodeVisible(Integer nodeId, Integer classroomId) {
        ClassroomNodeStatus nodeStatus = classroomNodeStatusRepository
                .findByClassroomIdAndNodeId(classroomId, nodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Node không tồn tại trong lớp này"));
        if (!NodeVisibilityStatus.VISIBLE.name().equals(nodeStatus.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Node này chưa được mở");
        }
    }
}