package com.edunac.mentora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeProgressResponse {
    private Integer nodeId;
    private String nodeTitle;
    private boolean completed;
    private int totalNodes;
    private int completedNodes;
    private double progressPercent;
}