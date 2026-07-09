package com.edunac.mentora.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StudentNodeLevelStatusView {

    private final Integer levelId;
    private final Integer levelNumber;
    private final String title;
    private final BigDecimal maxScore;
    private final BigDecimal passingScore;
    private final boolean hasQuestions;
    private final boolean unlocked;
    private final boolean hasAttemptsLeft;

    public StudentNodeLevelStatusView(
            Integer levelId,
            Integer levelNumber,
            String title,
            BigDecimal maxScore,
            BigDecimal passingScore,
            boolean hasQuestions,
            boolean unlocked,
            boolean hasAttemptsLeft
    ) {
        this.levelId = levelId;
        this.levelNumber = levelNumber;
        this.title = title;
        this.maxScore = maxScore;
        this.passingScore = passingScore;
        this.hasQuestions = hasQuestions;
        this.unlocked = unlocked;
        this.hasAttemptsLeft = hasAttemptsLeft;
    }

    public boolean isCanStart() {
        return hasQuestions && unlocked && hasAttemptsLeft;
    }
}
