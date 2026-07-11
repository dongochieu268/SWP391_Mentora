package com.edunac.mentora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MaterialForm {

    private Integer id;

    @NotNull(message = "Môn học không hợp lệ")
    private Integer subjectId;

    @NotBlank(message = "Tên material không được để trống")
    @Size(max = 255, message = "Tên material tối đa 255 ký tự")
    private String title;

    private String description;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
