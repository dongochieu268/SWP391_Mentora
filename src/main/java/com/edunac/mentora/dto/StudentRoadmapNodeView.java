package com.edunac.mentora.dto;

import com.edunac.mentora.domain.learningpath.LearningNode;
import lombok.Getter;

@Getter
public class StudentRoadmapNodeView {

    private final LearningNode node;
    private final StudentRoadmapNodeState state;
    private final String prerequisiteTitle;


    private final Boolean branchDecided;

    public StudentRoadmapNodeView(
            LearningNode node,
            StudentRoadmapNodeState state,
            String prerequisiteTitle) {
        this.node              = node;
        this.state             = state;
        this.prerequisiteTitle = prerequisiteTitle;
        this.branchDecided     = true;
    }

    public StudentRoadmapNodeView(
            LearningNode node,
            StudentRoadmapNodeState state,
            String prerequisiteTitle,
            Boolean branchDecided) {
        this.node              = node;
        this.state             = state;
        this.prerequisiteTitle = prerequisiteTitle;
        this.branchDecided     = branchDecided;
    }

    public boolean isLocked() {
        return state == StudentRoadmapNodeState.LOCKED;
    }
}