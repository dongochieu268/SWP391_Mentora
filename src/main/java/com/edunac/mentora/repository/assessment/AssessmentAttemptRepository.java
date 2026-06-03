package com.edunac.mentora.repository.assessment;

import com.edunac.mentora.domain.assessment.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Integer> {

    Optional<AssessmentAttempt> findFirstByAssessment_IdAndClassroom_IdAndStudent_IdAndStatusOrderBySubmittedAtDesc(
            Integer assessmentId,
            Integer classroomId,
            Integer studentId,
            String status
    );

    Optional<AssessmentAttempt> findFirstByAssessment_IdAndClassroom_IdAndStudent_IdAndStatusOrderByStartedAtDesc(
            Integer assessmentId,
            Integer classroomId,
            Integer studentId,
            String status
    );

    List<AssessmentAttempt> findByStudent_IdAndClassroom_IdOrderByStartedAtDesc(Integer studentId, Integer classroomId);
}
