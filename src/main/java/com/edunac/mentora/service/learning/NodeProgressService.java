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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NodeProgressService {

    private final NodeProgressRepository nodeProgressRepository;
    private final LearningNodeRepository learningNodeRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final ClassroomNodeStatusRepository classroomNodeStatusRepository;

    public NodeProgressResponse buildProgressResponse(Integer studentId, Integer classroomId, Integer nodeId) {

        boolean isMember = classroomMemberRepository
                .existsByClassroomIdAndUserIdAndStatus(classroomId, studentId, "ACTIVE");
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải thành viên của lớp này");
        }

        boolean isCompleted = nodeProgressRepository
                .existsByStudentIdAndLearningNodeIdAndClassroomIdAndCompletedTrue(studentId, nodeId, classroomId);

        long totalNodes = learningNodeRepository.countVisibleNodesByClassroom(classroomId);

        long completedNodes = nodeProgressRepository
                .countValidCompletedNodes(studentId, classroomId);

        completedNodes = Math.min(completedNodes, totalNodes);

        double percent = (totalNodes == 0) ? 0.0
                : Math.round((completedNodes * 100.0) / totalNodes * 10.0) / 10.0;

        LearningNode node = learningNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node không tồn tại"));

        boolean prerequisiteMet = true;
        if (node.getPrerequisite() != null) {
            prerequisiteMet = nodeProgressRepository
                    .existsByStudentIdAndLearningNodeIdAndClassroomIdAndCompletedTrue(
                            studentId, node.getPrerequisite().getId(), classroomId);
        }

        return NodeProgressResponse.builder()
                .completed(isCompleted)
                .completedNodes((int) completedNodes)
                .totalNodes((int) totalNodes)
                .progressPercent(percent)
                .prerequisiteMet(prerequisiteMet)
                .build();
    }

    @Transactional
    public NodeProgressResponse markNodeCompleted(Integer studentId, Integer nodeId, Integer classroomId) {

        boolean isMember = classroomMemberRepository
                .existsByClassroomIdAndUserIdAndStatus(classroomId, studentId, "ACTIVE");
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không phải thành viên của lớp này");
        }

        ClassroomNodeStatus nodeStatus = classroomNodeStatusRepository
                .findByClassroomIdAndNodeId(classroomId, nodeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Node không tồn tại trong lớp này"));

        if (!NodeVisibilityStatus.VISIBLE.name().equals(nodeStatus.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Node này chưa được mở");
        }

        LearningNode node = learningNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Node không tồn tại"));


        if (node.getPrerequisite() != null) {
            boolean prereqDone = nodeProgressRepository
                    .existsByStudentIdAndLearningNodeIdAndClassroomIdAndCompletedTrue(
                            studentId, node.getPrerequisite().getId(), classroomId);
            if (!prereqDone) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Bạn cần hoàn thành node tiên quyết trước: " + node.getPrerequisite().getTitle());
            }
        }

        boolean alreadyDone = nodeProgressRepository
                .existsByStudentIdAndLearningNodeIdAndClassroomIdAndCompletedTrue(studentId, nodeId, classroomId);

        if (!alreadyDone) {
            NodeProgress progress = new NodeProgress();

            User student = new User();
            student.setId(studentId);
            progress.setStudent(student);

            LearningNode nodeRef = new LearningNode();
            nodeRef.setId(nodeId);
            progress.setLearningNode(nodeRef);

            Classroom classroom = new Classroom();
            classroom.setId(classroomId);
            progress.setClassroom(classroom);

            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());

            nodeProgressRepository.save(progress);
        }

        return buildProgressResponse(studentId, classroomId, nodeId);
    }
}