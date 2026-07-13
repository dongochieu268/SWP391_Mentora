package com.edunac.mentora.service.level;

import com.edunac.mentora.domain.assessment.BankQuestion;
import com.edunac.mentora.domain.assessment.BankQuestionOption;
import com.edunac.mentora.domain.assessment.BankQuestionStatus;
import com.edunac.mentora.domain.level.AttemptQuestion;
import com.edunac.mentora.domain.level.AttemptQuestionOption;
import com.edunac.mentora.domain.level.LevelMaterial;
import com.edunac.mentora.domain.level.NodeLevelAttempt;
import com.edunac.mentora.repository.assessment.BankQuestionOptionRepository;
import com.edunac.mentora.repository.assessment.BankQuestionRepository;
import com.edunac.mentora.repository.level.AttemptQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NodeLevelQuestionService {

    private final BankQuestionRepository bankQuestionRepository;
    private final BankQuestionOptionRepository bankQuestionOptionRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;

    /**
     * Bốc đề ngẫu nhiên (re-drawn + re-shuffled mỗi attempt - OQ4) cho từng
     * LevelMaterial theo đúng questionCount (thiếu thì lấy hết - OQ7, no
     * cross-material borrowing), snapshot thành AttemptQuestion/
     * AttemptQuestionOption rồi lưu xuống DB.
     */
    @Transactional
    public void generateQuestionSnapshot(NodeLevelAttempt attempt, List<LevelMaterial> levelMaterials) {
        List<AttemptQuestion> attemptQuestions = new ArrayList<>();

        for (LevelMaterial levelMaterial : levelMaterials) {
            List<BankQuestion> pool = bankQuestionRepository
                    .findByMaterial_IdAndStatus(levelMaterial.getMaterial().getId(), BankQuestionStatus.ACTIVE.name());

            Collections.shuffle(pool);

            int takeCount = Math.min(levelMaterial.getQuestionCount(), pool.size());
            List<BankQuestion> selected = pool.subList(0, takeCount);

            for (BankQuestion bankQuestion : selected) {
                attemptQuestions.add(buildAttemptQuestion(attempt, bankQuestion));
            }
        }

        Collections.shuffle(attemptQuestions);
        for (int i = 0; i < attemptQuestions.size(); i++) {
            attemptQuestions.get(i).setDisplayOrder(i + 1);
        }

        attemptQuestionRepository.saveAll(attemptQuestions);
    }

    private AttemptQuestion buildAttemptQuestion(NodeLevelAttempt attempt, BankQuestion bankQuestion) {
        AttemptQuestion attemptQuestion = new AttemptQuestion();
        attemptQuestion.setAttempt(attempt);
        attemptQuestion.setSourceBankQuestion(bankQuestion);
        attemptQuestion.setContent(bankQuestion.getContent());
        attemptQuestion.setDifficulty(bankQuestion.getDifficulty());

        List<BankQuestionOption> optionPool = bankQuestionOptionRepository
                .findByBankQuestion_Id(bankQuestion.getId());
        Collections.shuffle(optionPool);

        List<AttemptQuestionOption> options = new ArrayList<>();
        for (int i = 0; i < optionPool.size(); i++) {
            BankQuestionOption source = optionPool.get(i);
            AttemptQuestionOption option = new AttemptQuestionOption();
            option.setAttemptQuestion(attemptQuestion);
            option.setContent(source.getContent());
            option.setCorrect(source.isCorrect());
            option.setDisplayOrder(i + 1);
            option.setSelected(false);
            options.add(option);
        }
        attemptQuestion.setOptions(options);

        return attemptQuestion;
    }
}