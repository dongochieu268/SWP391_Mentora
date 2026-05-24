package com.edunac.mentora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NodeContentForm {

    private Integer id;

    @NotNull(message = "Node không hợp lệ")
    private Integer nodeId;

    @NotBlank(message = "Loại nội dung không được để trống")
    private String contentType = "TEXT";

    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @Size(max = 1000, message = "URL tối đa 1000 ký tự")
    private String contentUrl;

    private String contentText;

    @NotNull(message = "Thứ tự hiển thị không hợp lệ")
    private Integer displayOrder = 1;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
