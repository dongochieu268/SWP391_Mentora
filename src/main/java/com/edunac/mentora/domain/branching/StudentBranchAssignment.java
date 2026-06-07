package com.edunac.mentora.domain.branching;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.learningpath.LearningNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "student_branch_assignments",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"student_id", "branch_node_id", "classroom_id"}
        )
)
@Getter
@Setter
public class StudentBranchAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_node_id", nullable = false)
    private LearningNode branchNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_branch", nullable = false, length = 10)
    private AssignedBranch assignedBranch;

    @Column(name = "attempt_score")
    private Integer attemptScore;

    @Column(name = "assigned_at", updatable = false)
    private LocalDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
    }
}