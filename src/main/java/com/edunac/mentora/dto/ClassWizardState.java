package com.edunac.mentora.dto;

import java.io.Serializable;

/** Trạng thái tạm của wizard "Tạo lớp học" — sống trong HttpSession, chỉ ghi DB ở bước xác nhận. */
public class ClassWizardState implements Serializable {

    private String name;
    private Integer subjectId;
    private Integer semesterId;
    private Integer learningPathId;

    public boolean step1Done() {
        return name != null && !name.isBlank() && subjectId != null && semesterId != null;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public Integer getSemesterId() { return semesterId; }
    public void setSemesterId(Integer semesterId) { this.semesterId = semesterId; }

    public Integer getLearningPathId() { return learningPathId; }
    public void setLearningPathId(Integer learningPathId) { this.learningPathId = learningPathId; }
}
