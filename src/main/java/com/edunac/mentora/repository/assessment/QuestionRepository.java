package com.edunac.mentora.repository.assessment;

import com.edunac.mentora.domain.assessment.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {

    List<Question> findByAssessment_IdOrderByIdAsc(Integer assessmentId);

    long countByAssessment_Id(Integer assessmentId);

    void deleteByAssessment_Id(Integer assessmentId);
}
