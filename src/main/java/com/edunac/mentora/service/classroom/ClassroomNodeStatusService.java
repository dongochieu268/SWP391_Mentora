package com.edunac.mentora.service.classroom;

import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import com.edunac.mentora.domain.classroom.NodeVisibilityStatus;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.repository.classroom.ClassroomNodeStatusRepository;
import com.edunac.mentora.repository.classroom.ClassroomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ClassroomNodeStatusService {

    private final ClassroomNodeStatusRepository nodeStatusRepository;
    private final ClassroomRepository classroomRepository;

    public ClassroomNodeStatusService(
            ClassroomNodeStatusRepository nodeStatusRepository,
            ClassroomRepository classroomRepository
    ) {
        this.nodeStatusRepository = nodeStatusRepository;
        this.classroomRepository = classroomRepository;
    }

    /**
     * Khi GV upload nội dung node → mở (VISIBLE) node cho mọi lớp dùng lộ trình đó.
     */
    public void openNodeForPathClassrooms(LearningNode node) {
        Integer pathId = node.getLearningPath().getId();
        List<Classroom> classrooms = classroomRepository.findByLearningPathId(pathId);
        for (Classroom classroom : classrooms) {
            openNodeForClassroom(classroom, node);
        }
    }

    private void openNodeForClassroom(Classroom classroom, LearningNode node) {
        ClassroomNodeStatus status = nodeStatusRepository
                .findByClassroomIdAndLearningNodeId(classroom.getId(), node.getId())
                .orElseGet(() -> {
                    ClassroomNodeStatus created = new ClassroomNodeStatus();
                    created.setClassroom(classroom);
                    created.setLearningNode(node);
                    return created;
                });

        if (!NodeVisibilityStatus.VISIBLE.name().equals(status.getStatus())) {
            status.setStatus(NodeVisibilityStatus.VISIBLE.name());
            status.setOpenedAt(LocalDateTime.now());
            nodeStatusRepository.save(status);
        }
    }
}
