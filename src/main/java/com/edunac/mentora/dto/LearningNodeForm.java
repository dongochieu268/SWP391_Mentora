package com.edunac.mentora.dto;

public class LearningNodeForm {

    private Integer id;
    private String title;
    private String description;
    private Integer afterNodeId;
    private Integer prerequisiteNodeId;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getAfterNodeId() { return afterNodeId; }
    public void setAfterNodeId(Integer afterNodeId) { this.afterNodeId = afterNodeId; }

    public Integer getPrerequisiteNodeId() { return prerequisiteNodeId; }
    public void setPrerequisiteNodeId(Integer prerequisiteNodeId) { this.prerequisiteNodeId = prerequisiteNodeId; }
}
