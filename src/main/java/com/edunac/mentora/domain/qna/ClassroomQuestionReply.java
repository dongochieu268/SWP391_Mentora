package com.edunac.mentora.domain.qna;

import com.edunac.mentora.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "classroom_question_replies")
@Getter
@Setter
public class ClassroomQuestionReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "question_id", nullable = false)
    private ClassroomQuestion question;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responder_id", nullable = false)
    private User responder;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private boolean official;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Transient
    private String responderRoleLabel;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
