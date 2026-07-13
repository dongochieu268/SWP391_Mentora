package com.edunac.mentora.domain.level;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.assessment.AttemptStatus;
import com.edunac.mentora.domain.classroom.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One student's single attempt at one level. Status reuses the existing
 * AttemptStatus (IN_PROGRESS, SUBMITTED) — grading is immediate at submit.
 * Selected options are persisted on AttemptQuestionOption (rev 2026-07-05).
 */
@Entity
@Table(name = "node_level_attempts",
        uniqueConstraints = @UniqueConstraint(name = "uq_level_attempt",
                columnNames = {"node_level_id", "student_id", "classroom_id", "attempt_number"}))
@Getter
@Setter
public class NodeLevelAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "node_level_id", nullable = false)
    private NodeLevel nodeLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;   // NULL until submitted

    @Column(nullable = false, length = 20)
    private String status = AttemptStatus.IN_PROGRESS.name();

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // Evaluated against passingScore AT SUBMIT TIME, then immutable — later
    // changes to the level's passingScore never rewrite past attempts (L3 §9).
    @Column(nullable = false)
    private boolean passed;

    // Set ONLY by the timeout auto-finalize path; display-only until
    // durationMinutes enforcement ships (deferred post-defense, rev 06b).
    @Column(name = "timed_out", nullable = false)
    private boolean timedOut;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
        if (status == null || status.isBlank()) status = AttemptStatus.IN_PROGRESS.name();
    }

    public boolean isSubmitted() {
        return AttemptStatus.SUBMITTED.name().equals(status);
    }

    public boolean isInProgress() {
        return AttemptStatus.IN_PROGRESS.name().equals(status);
    }
}
