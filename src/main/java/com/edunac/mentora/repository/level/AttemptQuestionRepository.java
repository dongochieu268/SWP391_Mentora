package com.edunac.mentora.repository.level;

import com.edunac.mentora.domain.level.AttemptQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, Integer> {

    List<AttemptQuestion> findByAttempt_IdOrderByDisplayOrderAsc(Integer attemptId);

    @Query("""
            select distinct question
            from AttemptQuestion question
            left join fetch question.options
            where question.attempt.id = :attemptId
            order by question.displayOrder asc
            """)
    List<AttemptQuestion> findWithOptionsByAttemptId(@Param("attemptId") Integer attemptId);

    long countByAttempt_Id(Integer attemptId);
}