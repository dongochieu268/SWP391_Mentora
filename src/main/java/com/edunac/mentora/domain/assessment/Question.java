package com.edunac.mentora.domain.assessment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "questions")
@Getter
@Setter
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false, length = 20)
    private String difficulty;

    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType = QuestionType.MULTIPLE_CHOICE.name();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal score;
}
