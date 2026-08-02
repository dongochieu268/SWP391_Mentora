package com.edunac.mentora.dto;

public class LearningNodeForm {

    private Integer id;
    private String title;
    private String description;
    private Integer prerequisiteNodeId;

    private String nodeType = "LESSON";
    private String branchTag = "MAIN";
    private Integer branchOwnerNodeId;
    private Integer assessmentId;
    private Double minScore;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPrerequisiteNodeId() { return prerequisiteNodeId; }
    public void setPrerequisiteNodeId(Integer prerequisiteNodeId) { this.prerequisiteNodeId = prerequisiteNodeId; }

    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }

    public String getBranchTag() { return branchTag; }
    public void setBranchTag(String branchTag) { this.branchTag = branchTag; }

    public Integer getBranchOwnerNodeId() { return branchOwnerNodeId; }
    public void setBranchOwnerNodeId(Integer branchOwnerNodeId) { this.branchOwnerNodeId = branchOwnerNodeId; }

    public Integer getAssessmentId() { return assessmentId; }
    public void setAssessmentId(Integer assessmentId) { this.assessmentId = assessmentId; }

    public Double getMinScore() { return minScore; }
    public void setMinScore(Double minScore) { this.minScore = minScore; }
}