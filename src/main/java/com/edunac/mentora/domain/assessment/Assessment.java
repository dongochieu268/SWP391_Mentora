package com.edunac.mentora.domain.assessment;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.subject.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessments")
@Getter
@Setter
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 30)
    private String type = AssessmentType.QUIZ.name();

    @Column(name = "delivery_mode", nullable = false, length = 30)
    private String deliveryMode = DeliveryMode.SELF_PACED.name();

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "total_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalScore;

    @Column(nullable = false, length = 20)
    private String status = AssessmentStatus.DRAFT.name();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = AssessmentStatus.DRAFT.name();
        }
        if (type == null || type.isBlank()) {
            type = AssessmentType.QUIZ.name();
        }
        if (deliveryMode == null || deliveryMode.isBlank()) {
            deliveryMode = DeliveryMode.SELF_PACED.name();
        }
    }
}
