package com.edunac.mentora.repository.learning;

import com.edunac.mentora.domain.learning.NodeProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NodeProgressRepository extends JpaRepository<NodeProgress, Integer> {

    List<NodeProgress> findByClassroomIdAndStudentId(Integer classroomId, Integer studentId);
}
