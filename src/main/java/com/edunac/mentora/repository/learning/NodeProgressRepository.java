package com.edunac.mentora.repository.learning;

import com.edunac.mentora.domain.learning.NodeProgress; // Đã sửa đường dẫn import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeProgressRepository extends JpaRepository<NodeProgress, Integer> {

    Optional<NodeProgress> findByStudentIdAndNodeIdAndClassroomId(
            Integer studentId, Integer nodeId, Integer classroomId);

    List<NodeProgress> findByStudentIdAndClassroomId(
            Integer studentId, Integer classroomId);

    long countByStudentIdAndClassroomIdAndIsCompletedTrue(
            Integer studentId, Integer classroomId);

    boolean existsByStudentIdAndNodeIdAndClassroomIdAndIsCompletedTrue(
            Integer studentId, Integer nodeId, Integer classroomId);
}