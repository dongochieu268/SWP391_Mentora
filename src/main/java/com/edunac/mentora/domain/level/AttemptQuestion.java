package com.edunac.mentora.domain.level;

import com.edunac.mentora.domain.assessment.BankQuestion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-attempt snapshot of a generated question. Content is copied from
 * BankQuestion at generation time so later bank edits never affect
 * in-flight or historical attempts. sourceBankQuestion is traceability
 * only and may be null (e.g. after the bank question is deleted).
 */
@Entity
@Table(name = "attempt_questions")
@Getter
@Setter
public class AttemptQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private NodeLevelAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_bank_question_id")
    private BankQuestion sourceBankQuestion;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @OneToMany(mappedBy = "attemptQuestion", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<AttemptQuestionOption> options = new ArrayList<>();
}
