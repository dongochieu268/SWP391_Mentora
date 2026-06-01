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

@Service
@RequiredArgsConstructor
public class NodeProgressService {

    private final NodeProgressRepository nodeProgressRepository;
    private final LearningNodeRepository learningNodeRepository;

    public NodeProgressResponse buildProgressResponse(Integer studentId, Integer classroomId, Integer nodeId) {
        boolean isCompleted = nodeProgressRepository
                .existsByStudentIdAndLearningNodeIdAndClassroomIdAndCompletedTrue(studentId, nodeId, classroomId);

        long totalNodes = learningNodeRepository.countVisibleNodesByClassroom(classroomId);
        long completedNodes = nodeProgressRepository.countByStudentIdAndClassroomIdAndCompletedTrue(studentId, classroomId);

        double percent = (totalNodes == 0) ? 0.0 : Math.round((completedNodes * 100.0) / totalNodes * 10.0) / 10.0;

        return NodeProgressResponse.builder()
                .completed(isCompleted)
                .completedNodes((int) completedNodes)
                .totalNodes((int) totalNodes)
                .progressPercent(percent)
                .build();
    }

    @Transactional
    public NodeProgressResponse markNodeCompleted(Integer studentId, Integer nodeId, Integer classroomId) {
        boolean exists = nodeProgressRepository
                .existsByStudentIdAndLearningNodeIdAndClassroomIdAndCompletedTrue(studentId, nodeId, classroomId);

        if (!exists) {
            NodeProgress progress = new NodeProgress();

            User student = new User();
            student.setId(studentId);
            progress.setStudent(student);

            LearningNode node = new LearningNode();
            node.setId(nodeId);
            progress.setLearningNode(node);

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