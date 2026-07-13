package com.edunac.mentora.dto;

import lombok.Getter;
import lombok.Setter;

/** Form gắn một Material vào một NodeLevel với số câu hỏi cụ thể. */
@Getter
@Setter
public class LevelMaterialForm {
    private Integer levelId;
    private Integer materialId;
    private Integer questionCount;
}
