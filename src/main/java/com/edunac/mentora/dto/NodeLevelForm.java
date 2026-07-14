package com.edunac.mentora.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** Form dùng cho cả tạo mới lẫn chỉnh sửa NodeLevel. id == null → tạo mới. */
@Getter
@Setter
public class NodeLevelForm {
    private Integer id;
    private Integer nodeId;
    private Integer levelNumber;
    private String title;
    private BigDecimal maxScore;
    private BigDecimal passingScore;
    private Integer maxAttempts;      // null = unlimited
    private Integer durationMinutes;  // null = no time limit
}
