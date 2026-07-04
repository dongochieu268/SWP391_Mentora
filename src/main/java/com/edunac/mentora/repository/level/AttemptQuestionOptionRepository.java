package com.edunac.mentora.repository.level;

import com.edunac.mentora.domain.level.AttemptQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttemptQuestionOptionRepository extends JpaRepository<AttemptQuestionOption, Integer> {

    List<AttemptQuestionOption> findByAttemptQuestion_IdOrderByDisplayOrderAsc(Integer attemptQuestionId);

    List<AttemptQuestionOption> findByAttemptQuestion_Attempt_Id(Integer attemptId);
}
