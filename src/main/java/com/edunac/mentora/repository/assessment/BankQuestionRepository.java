package com.edunac.mentora.repository.assessment;

import com.edunac.mentora.domain.assessment.BankQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BankQuestionRepository extends JpaRepository<BankQuestion, Integer> {
    List<BankQuestion> findByQuestionBank_Subject_IdAndStatusOrderByUpdatedAtDesc(Integer subjectId, String status);
    List<BankQuestion> findByIdIn(Collection<Integer> ids);

    long countByMaterial_IdAndStatus(Integer materialId, String status);
    List<BankQuestion> findByMaterial_IdAndStatus(Integer materialId, String status);


    @Query("""
            select bankQuestion
            from BankQuestion bankQuestion
            where bankQuestion.questionBank.subject.id = :subjectId
              and bankQuestion.status = :status
              and (:keyword = '' or lower(bankQuestion.content) like lower(concat('%', :keyword, '%')))
              and (:difficulty = '' or bankQuestion.difficulty = :difficulty)
              and not exists (
                  select question.id
                  from Question question
                  where question.assessment.id = :assessmentId
                    and question.sourceBankQuestion.id = bankQuestion.id
              )
            order by bankQuestion.updatedAt desc
            """)
    Page<BankQuestion> searchAvailableForAssessment(
            @Param("assessmentId") Integer assessmentId,
            @Param("subjectId") Integer subjectId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("difficulty") String difficulty,
            Pageable pageable
    );
}
