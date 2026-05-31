package com.edunac.mentora.service.learning;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.learning.NodeProgress;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.dto.NodeProgressResponse;
import com.edunac.mentora.repository.learning.NodeProgressRepository;
import com.edunac.mentora.repository.learningpath.LearningNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeProgressService {

    private final NodeProgressRepository nodeProgressRepository;
    private final LearningNodeRepository learningNodeRepository;

    @Transactional
    public NodeProgressResponse markNodeCompleted(Integer studentId, Integer nodeId, Integer classroomId) {
        NodeProgress progress = nodeProgressRepository
                .findByStudent_IdAndLearningNode_IdAndClassroom_Id(studentId, nodeId, classroomId)
                .orElseGet(() -> createProgress(studentId, nodeId, classroomId));

        if (!progress.isCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            nodeProgressRepository.save(progress);
        }

        return buildProgressResponse(studentId, classroomId, nodeId);
    }

    @Transactional(readOnly = true)
    public NodeProgressResponse buildProgressResponse(Integer studentId, Integer classroomId, Integer nodeId) {
        List<LearningNode> visibleNodes = learningNodeRepository.findVisibleNodesByClassroom(classroomId);
        int totalNodes = visibleNodes.size();

        long completedNodes = nodeProgressRepository
                .countByStudent_IdAndClassroom_IdAndCompletedTrue(studentId, classroomId);

        double percent = totalNodes == 0 ? 0.0
                : Math.round(completedNodes * 100.0 / totalNodes * 10.0) / 10.0;

        LearningNode currentNode = learningNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node không tồn tại: " + nodeId));

        boolean isCompleted = nodeProgressRepository
                .existsByStudent_IdAndLearningNode_IdAndClassroom_IdAndCompletedTrue(
                        studentId, nodeId, classroomId);

        return NodeProgressResponse.builder()
                .nodeId(nodeId)
                .nodeTitle(currentNode.getTitle())
                .completed(isCompleted)
                .totalNodes(totalNodes)
                .completedNodes((int) completedNodes)
                .progressPercent(percent)
                .build();
    }

    @Transactional(readOnly = true)
    public List<NodeProgress> getProgressByStudentAndClassroom(Integer studentId, Integer classroomId) {
        return nodeProgressRepository.findByStudent_IdAndClassroom_Id(studentId, classroomId);
    }

    private NodeProgress createProgress(Integer studentId, Integer nodeId, Integer classroomId) {
        NodeProgress progress = new NodeProgress();

        User student = new User();
        student.setId(studentId);
        progress.setStudent(student);

        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        progress.setClassroom(classroom);

        LearningNode node = new LearningNode();
        node.setId(nodeId);
        progress.setLearningNode(node);

        progress.setCompleted(false);
        return progress;
    }
}
