package com.edunac.mentora.dto;

import com.edunac.mentora.domain.learningpath.LearningNode;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StudentRoadmapNodeView {

    private final LearningNode node;
    private final StudentRoadmapNodeState state;
    private final String prerequisiteTitle;
    private final int totalLevels;
    private final Integer bestLevelNumber;
    private final BigDecimal bestScore;

    public StudentRoadmapNodeView(
            LearningNode node,
            StudentRoadmapNodeState state,
            String prerequisiteTitle,
            int totalLevels,
            Integer bestLevelNumber,
            BigDecimal bestScore) {
        this.node              = node;
        this.state             = state;
        this.prerequisiteTitle = prerequisiteTitle;
        this.totalLevels       = totalLevels;
        this.bestLevelNumber   = bestLevelNumber;
        this.bestScore         = bestScore;
    }

    public boolean isLocked() {
        return state == StudentRoadmapNodeState.LOCKED;
    }

    public boolean isHasLevels() {
        return totalLevels > 0;
    }
}