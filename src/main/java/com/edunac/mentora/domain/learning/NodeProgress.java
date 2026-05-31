package com.edunac.mentora.domain.learning;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.learningpath.LearningNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "node_progress",
        uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_id", "student_id", "learning_node_id"}))
@Getter
@Setter
@NoArgsConstructor
public class NodeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "learning_node_id", nullable = false)
    private LearningNode learningNode;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Tương thích code merge (StudentLearningController, NodeProgressService). */
    public Integer getNodeId() {
        return learningNode != null ? learningNode.getId() : null;
    }

    public Integer getStudentId() {
        return student != null ? student.getId() : null;
    }

    public Integer getClassroomId() {
        return classroom != null ? classroom.getId() : null;
    }

    public Boolean getIsCompleted() {
        return completed;
    }

    public void setIsCompleted(Boolean value) {
        this.completed = Boolean.TRUE.equals(value);
    }
}
