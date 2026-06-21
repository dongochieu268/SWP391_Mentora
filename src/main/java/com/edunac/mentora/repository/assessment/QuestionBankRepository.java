package com.edunac.mentora.repository.assessment;

import com.edunac.mentora.domain.assessment.QuestionBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionBankRepository extends JpaRepository<QuestionBank, Integer> {
    Optional<QuestionBank> findBySubject_Id(Integer subjectId);
}
