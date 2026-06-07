package com.edunac.mentora.service.branching;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.branching.AssignedBranch;
import com.edunac.mentora.domain.branching.BranchRule;
import com.edunac.mentora.domain.branching.StudentBranchAssignment;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.learningpath.LearningNode;
import com.edunac.mentora.repository.branching.BranchRuleRepository;
import com.edunac.mentora.repository.branching.StudentBranchAssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentBranchService {

    private final BranchRuleRepository branchRuleRepository;
    private final StudentBranchAssignmentRepository assignmentRepository;

    @Transactional
    public Optional<StudentBranchAssignment> assignBranch(
            Integer assessmentId,
            Integer classroomId,
            Integer pathId,
            Integer studentId,
            Integer score) {

        Optional<BranchRule> ruleOpt = branchRuleRepository.findByAssessmentAndPath(assessmentId, pathId);
        if (ruleOpt.isEmpty()) {
            return Optional.empty();
        }

        BranchRule rule = ruleOpt.get();
        Integer branchNodeId = rule.getNode().getId();

        StudentBranchAssignment assignment = assignmentRepository
                .findByStudentIdAndBranchNodeIdAndClassroomId(studentId, branchNodeId, classroomId)
                .orElse(new StudentBranchAssignment());

        AssignedBranch branch = (score >= rule.getMinScore())
                ? AssignedBranch.PASS
                : AssignedBranch.FAIL;

        User student = new User();
        student.setId(studentId);
        assignment.setStudent(student);

        LearningNode branchNode = new LearningNode();
        branchNode.setId(branchNodeId);
        assignment.setBranchNode(branchNode);

        Classroom classroom = new Classroom();
        classroom.setId(classroomId);
        assignment.setClassroom(classroom);

        assignment.setAssignedBranch(branch);
        assignment.setAttemptScore(score);

        return Optional.of(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<StudentBranchAssignment> getAssignments(Integer studentId, Integer classroomId) {
        return assignmentRepository.findByStudentIdAndClassroomId(studentId, classroomId);
    }

    @Transactional(readOnly = true)
    public Optional<StudentBranchAssignment> getAssignment(
            Integer studentId, Integer branchNodeId, Integer classroomId) {
        return assignmentRepository.findByStudentIdAndBranchNodeIdAndClassroomId(
                studentId, branchNodeId, classroomId);
    }
}