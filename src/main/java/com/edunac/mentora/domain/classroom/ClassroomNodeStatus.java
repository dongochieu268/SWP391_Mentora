package com.edunac.mentora.domain.classroom;

import com.edunac.mentora.domain.learningpath.LearningNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "classroom_node_status",

        uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_id", "node_id"}))
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
    private LearningNode learningNode;

    @Column(nullable = false, length = 20)
    private String status = NodeVisibilityStatus.HIDDEN.name();

    @Column(name = "updated_at")
    private LocalDateTime openedAt;

    @PrePersist
    protected void onCreate() {
        if (NodeVisibilityStatus.VISIBLE.name().equals(status) && openedAt == null) {
            openedAt = LocalDateTime.now();
        }
    }
}