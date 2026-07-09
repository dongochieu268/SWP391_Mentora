package com.edunac.mentora.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StudentNodeLevelHistoryView {

    private final Integer levelNumber;
    private final String title;
    private final BigDecimal maxScore;
    private final BigDecimal passingScore;
    private final int attemptCount;
    private final BigDecimal bestScore;
    private final boolean passed;

    public StudentNodeLevelHistoryView(
            Integer levelNumber,
            String title,
            BigDecimal maxScore,
            BigDecimal passingScore,
            int attemptCount,
            BigDecimal bestScore,
            boolean passed
    ) {
        this.levelNumber = levelNumber;
        this.title = title;
        this.maxScore = maxScore;
        this.passingScore = passingScore;
        this.attemptCount = attemptCount;
        this.bestScore = bestScore;
        this.passed = passed;
    }

    public boolean isAttempted() {
        return attemptCount > 0;
    }
}
