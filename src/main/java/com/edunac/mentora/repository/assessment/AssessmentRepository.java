package com.edunac.mentora.repository.assessment;

import com.edunac.mentora.domain.assessment.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {

    List<Assessment> findByCreatedByIdOrderByCreatedAtDesc(Integer createdById);

    List<Assessment> findByCreatedByIdAndStatusOrderByCreatedAtDesc(Integer createdById, String status);
}
