package com.edunac.mentora.repository.assessment;

import com.edunac.mentora.domain.assessment.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Integer> {

    List<QuestionOption> findByQuestion_IdOrderByIdAsc(Integer questionId);

    List<QuestionOption> findByQuestion_IdInOrderByQuestion_IdAscIdAsc(Collection<Integer> questionIds);

    long countByQuestion_Id(Integer questionId);

    long countByQuestion_IdAndCorrectTrue(Integer questionId);

    void deleteByQuestion_Id(Integer questionId);
}
