package com.edunac.mentora.repository.branching;

import com.edunac.mentora.domain.branching.StudentBranchAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentBranchAssignmentRepository extends JpaRepository<StudentBranchAssignment, Integer> {

    Optional<StudentBranchAssignment> findByStudentIdAndBranchNodeIdAndClassroomId(
            Integer studentId, Integer branchNodeId, Integer classroomId);

    List<StudentBranchAssignment> findByStudentIdAndClassroomId(
            Integer studentId, Integer classroomId);

    boolean existsByStudentIdAndBranchNodeIdAndClassroomId(
            Integer studentId, Integer branchNodeId, Integer classroomId);
}