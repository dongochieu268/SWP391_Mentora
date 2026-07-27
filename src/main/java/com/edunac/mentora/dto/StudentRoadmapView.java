package com.edunac.mentora.dto;

import com.edunac.mentora.domain.classroom.Classroom;
import lombok.Getter;

import java.util.List;

@Getter
public class StudentRoadmapView {

    private final Classroom classroom;
    private final List<StudentRoadmapNodeView> nodes;
    private final int completionPercent;
    private final int visibleNodeCount;
    private final int completedVisibleCount;

    public StudentRoadmapView(
            Classroom classroom,
            List<StudentRoadmapNodeView> nodes,
            int completionPercent,
            int visibleNodeCount,
            int completedVisibleCount
    ) {
        this.classroom = classroom;
        this.nodes = nodes;
        this.completionPercent = completionPercent;
        this.visibleNodeCount = visibleNodeCount;
        this.completedVisibleCount = completedVisibleCount;
    }
}
