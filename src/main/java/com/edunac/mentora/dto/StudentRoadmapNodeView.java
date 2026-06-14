package com.edunac.mentora.dto;

import com.edunac.mentora.domain.learningpath.LearningNode;
import lombok.Getter;

@Getter
public class StudentRoadmapNodeView {

    private final LearningNode node;
    private final StudentRoadmapNodeState state;
    private final String prerequisiteTitle;

    /**
     * null  = chưa làm bài test (chưa biết sẽ vào nhánh nào)
     * true  = đúng nhánh / node MAIN
     * false = sai nhánh (hiện tại bị filter trước khi đến view, field này dùng để debug)
     */
    private final Boolean branchDecided;

    // Constructor cũ — giữ để không break code khác
    public StudentRoadmapNodeView(
            LearningNode node,
            StudentRoadmapNodeState state,
            String prerequisiteTitle) {
        this.node              = node;
        this.state             = state;
        this.prerequisiteTitle = prerequisiteTitle;
        this.branchDecided     = true;
    }

    // Constructor mới dùng bởi StudentRoadmapService sau khi thêm filter
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

    /** Tiện ích dùng trong Thymeleaf: ${item.locked} */
    public boolean isLocked() {
        return state == StudentRoadmapNodeState.LOCKED;
    }
}