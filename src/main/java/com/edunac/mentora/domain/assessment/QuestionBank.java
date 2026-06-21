package com.edunac.mentora.domain.assessment;

import com.edunac.mentora.domain.subject.Subject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_banks", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_question_banks_subject", columnNames = "subject_id")
})
@Getter
@Setter
public class QuestionBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 20)
    private String status = QuestionBankStatus.ACTIVE.name();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (status == null || status.isBlank()) status = QuestionBankStatus.ACTIVE.name();
    }
}
