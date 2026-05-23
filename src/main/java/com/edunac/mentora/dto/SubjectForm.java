package com.edunac.mentora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubjectForm {

    private Integer id;

    @NotBlank(message = "Mã môn không được để trống")
    @Size(max = 20, message = "Mã môn tối đa 20 ký tự")
    private String code;

    @NotBlank(message = "Tên môn không được để trống")
    @Size(max = 200, message = "Tên môn tối đa 200 ký tự")
    private String name;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    private String status = "ACTIVE";

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
