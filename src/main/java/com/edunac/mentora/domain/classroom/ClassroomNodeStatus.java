package com.edunac.mentora.domain.classroom;

import com.edunac.mentora.domain.learningpath.LearningNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "classroom_node_status")
@Getter
@Setter
public class ClassroomNodeStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "node_id", nullable = false)
    private LearningNode node;

    @Column(nullable = false, length = 20)
    private String status = NodeVisibilityStatus.HIDDEN.name();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
        if (status == null || status.isBlank()) {
            status = NodeVisibilityStatus.HIDDEN.name();
        }
    }
}