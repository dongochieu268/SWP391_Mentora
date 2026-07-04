package com.edunac.mentora.domain.level;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Snapshot of one answer option, copied from BankQuestionOption at
 * generation time; display order is shuffled per attempt.
 */
@Entity
@Table(name = "attempt_question_options")
@Getter
@Setter
public class AttemptQuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_question_id", nullable = false)
    private AttemptQuestion attemptQuestion;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
