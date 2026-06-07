package com.edunac.mentora.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClassroomNodeView {

    private final Integer nodeId;
    private final String title;
    private final String description;
    private final String nodeOrder;
    private final String prerequisiteTitle;
    private final String status;
}
