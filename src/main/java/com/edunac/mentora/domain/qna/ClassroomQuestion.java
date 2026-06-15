package com.edunac.mentora.domain.qna;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "classroom_questions")
@Getter
@Setter
public class ClassroomQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false, length = 20)
    private String status = QuestionStatus.OPEN.name();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "answered_by")
    private User answeredBy;

    @Column(name = "answer_content", length = 2000)
    private String answerContent;

    @OneToMany(mappedBy = "question", fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    private List<ClassroomQuestionReply> replies = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = QuestionStatus.OPEN.name();
        }
    }
}
