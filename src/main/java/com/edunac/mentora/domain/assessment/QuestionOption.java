package com.edunac.mentora.domain.assessment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "question_options")
@Getter
@Setter
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;
}
