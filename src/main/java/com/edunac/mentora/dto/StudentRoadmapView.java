package com.edunac.mentora.dto;

import com.edunac.mentora.domain.classroom.Classroom;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class StudentRoadmapView {

    private final Classroom classroom;
    private final List<StudentRoadmapNodeView> nodes;
    private final int completionPercent;
    private final int visibleNodeCount;
    private final int completedVisibleCount;
    private final BigDecimal totalScoreEarned;
    private final BigDecimal totalScorePossible;
    private final int scorePercent;

    public StudentRoadmapView(
            Classroom classroom,
            List<StudentRoadmapNodeView> nodes,
            int completionPercent,
            int visibleNodeCount,
            int completedVisibleCount,
            BigDecimal totalScoreEarned,
            BigDecimal totalScorePossible,
            int scorePercent
    ) {
        this.classroom = classroom;
        this.nodes = nodes;
        this.completionPercent = completionPercent;
        this.visibleNodeCount = visibleNodeCount;
        this.completedVisibleCount = completedVisibleCount;
        this.totalScoreEarned = totalScoreEarned;
        this.totalScorePossible = totalScorePossible;
        this.scorePercent = scorePercent;
    }

    public boolean isHasScorableNodes() {
        return totalScorePossible.compareTo(BigDecimal.ZERO) > 0;
    }
}
