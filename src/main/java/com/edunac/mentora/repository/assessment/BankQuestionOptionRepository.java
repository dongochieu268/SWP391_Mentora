package com.edunac.mentora.repository.assessment;

import com.edunac.mentora.domain.assessment.BankQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BankQuestionOptionRepository extends JpaRepository<BankQuestionOption, Integer> {
    List<BankQuestionOption> findByBankQuestion_IdOrderByDisplayOrderAscIdAsc(Integer bankQuestionId);
    List<BankQuestionOption> findByBankQuestion_IdInOrderByBankQuestion_IdAscDisplayOrderAscIdAsc(Collection<Integer> ids);
    void deleteByBankQuestion_Id(Integer bankQuestionId);
    List<BankQuestionOption> findByBankQuestion_Id(Integer bankQuestionId);

}
