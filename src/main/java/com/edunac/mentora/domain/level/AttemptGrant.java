package com.edunac.mentora.domain.level;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Extra attempts granted by the lecturer to one student for one level in
 * one classroom (L7). Solves "em lỡ tay nộp / ốm, hết lượt" per student
 * instead of the class-wide workarounds (raising maxAttempts or lowering
 * passingScore, which affect everyone). Never deleted by the UI — audit
 * trail via grantedBy/grantedAt.
 */
@Entity
@Table(name = "attempt_grants")
@Getter
@Setter
public class AttemptGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_level_id", nullable = false)
    private NodeLevel nodeLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @Column(name = "extra_attempts", nullable = false)
    private Integer extraAttempts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) grantedAt = LocalDateTime.now();
    }
}
