package com.edunac.mentora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeProgressResponse {
    private boolean completed;
    private int completedNodes;
    private int totalNodes;
    private double progressPercent;
    private boolean prerequisiteMet;
}