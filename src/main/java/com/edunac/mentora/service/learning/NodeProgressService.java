package com.edunac.mentora.service.learning;

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
                .findByStudentIdAndNodeIdAndClassroomId(studentId, nodeId, classroomId)
                .orElseGet(() -> NodeProgress.builder()
                        .studentId(studentId)
                        .nodeId(nodeId)
                        .classroomId(classroomId)
                        .isCompleted(false)
                        .build());

        if (Boolean.FALSE.equals(progress.getIsCompleted())) {
            progress.setIsCompleted(true);
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
                .countByStudentIdAndClassroomIdAndIsCompletedTrue(studentId, classroomId);

        double percent = totalNodes == 0 ? 0.0
                : Math.round(completedNodes * 100.0 / totalNodes * 10.0) / 10.0;

        LearningNode currentNode = learningNodeRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("Node không tồn tại: " + nodeId));

        boolean isCompleted = nodeProgressRepository
                .existsByStudentIdAndNodeIdAndClassroomIdAndIsCompletedTrue(studentId, nodeId, classroomId);

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
        return nodeProgressRepository.findByStudentIdAndClassroomId(studentId, classroomId);
    }
}