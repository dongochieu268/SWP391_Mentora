package com.edunac.mentora.domain.level;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_level_reviews", uniqueConstraints = @UniqueConstraint(
        name = "uq_student_level_review", columnNames = {"node_level_id", "student_id", "classroom_id"}))
@Getter @Setter
public class StudentLevelReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "node_level_id", nullable = false) private NodeLevel nodeLevel;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false) private User student;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "classroom_id", nullable = false) private Classroom classroom;
    @Column(name = "reviewed_at", nullable = false) private LocalDateTime reviewedAt;
    @PrePersist void onCreate() { if (reviewedAt == null) reviewedAt = LocalDateTime.now(); }
}
