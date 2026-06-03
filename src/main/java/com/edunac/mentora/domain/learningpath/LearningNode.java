package com.edunac.mentora.domain.learningpath;

import com.edunac.mentora.domain.learning.NodeContent;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "learning_nodes")
@Getter
@Setter
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

    @Column(name = "node_order", nullable = false, precision = 18, scale = 9)
    private BigDecimal nodeOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "prerequisite_node_id")
    private LearningNode prerequisite;

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    private List<NodeContent> contents = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
