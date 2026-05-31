package com.edunac.mentora.repository.learning;

import com.edunac.mentora.domain.learning.NodeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeProgressRepository extends JpaRepository<NodeProgress, Integer> {

    List<NodeProgress> findByClassroomIdAndStudentId(Integer classroomId, Integer studentId);

    boolean existsByStudentIdAndLearningNodeIdAndClassroomIdAndCompletedTrue(Integer studentId, Integer learningNodeId, Integer classroomId);

    long countByStudentIdAndClassroomIdAndCompletedTrue(Integer studentId, Integer classroomId);
}