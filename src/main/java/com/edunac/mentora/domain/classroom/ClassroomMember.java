package com.edunac.mentora.domain.classroom;

import com.edunac.mentora.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "classroom_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_id", "user_id"}))
@Getter
@Setter
public class ClassroomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_in_class", nullable = false, length = 20)
    private String roleInClass = MemberRole.STUDENT.name();

    @Column(nullable = false, length = 20)
    private String status = MemberStatus.PENDING.name();

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}
