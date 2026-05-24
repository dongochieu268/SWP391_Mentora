package com.edunac.mentora.domain.learning;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_nodes")
public class LearningNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_path_id", nullable = false)
    private LearningPath learningPath;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "node_order", nullable = false)
    private Double nodeOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_node_id")
    private LearningNode prerequisiteNode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LearningPath getLearningPath() {
        return learningPath;
    }

    public void setLearningPath(LearningPath learningPath) {
        this.learningPath = learningPath;
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

    public Double getNodeOrder() {
        return nodeOrder;
    }

    public void setNodeOrder(Double nodeOrder) {
        this.nodeOrder = nodeOrder;
    }

    public LearningNode getPrerequisiteNode() {
        return prerequisiteNode;
    }

    public void setPrerequisiteNode(LearningNode prerequisiteNode) {
        this.prerequisiteNode = prerequisiteNode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
