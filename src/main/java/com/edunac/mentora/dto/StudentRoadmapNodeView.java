package com.edunac.mentora.dto;

import com.edunac.mentora.domain.learningpath.LearningNode;
import lombok.Getter;

@Getter
public class StudentRoadmapNodeView {

    private final LearningNode node;
    private final StudentRoadmapNodeState state;
    private final String prerequisiteTitle;

    public StudentRoadmapNodeView(LearningNode node, StudentRoadmapNodeState state, String prerequisiteTitle) {
        this.node = node;
        this.state = state;
        this.prerequisiteTitle = prerequisiteTitle;
    }

    public boolean isLocked() {
        return state == StudentRoadmapNodeState.LOCKED || state == StudentRoadmapNodeState.HIDDEN;
    }

    public boolean isAccessible() {
        return state == StudentRoadmapNodeState.ACCESSIBLE || state == StudentRoadmapNodeState.COMPLETED;
    }
}
