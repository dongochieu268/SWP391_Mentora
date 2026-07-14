package com.edunac.mentora.dto;

import com.edunac.mentora.domain.learning.NodeContent;
import com.edunac.mentora.domain.level.Material;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/** Material learning content shown before the configured level test. */
@Getter
@AllArgsConstructor
public class StudentLevelMaterialView {
    private final Integer levelId;
    private final Integer levelNumber;
    private final String levelTitle;
    private final Material material;
    private final List<NodeContent> contents;
    private final boolean unlocked;
    private final boolean reviewed;
}
