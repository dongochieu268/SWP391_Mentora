package com.edunac.mentora.repository.classroom;

import com.edunac.mentora.domain.classroom.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Integer> {

    List<Classroom> findByTeacherIdOrderByCreatedAtDesc(Integer teacherId);

    Optional<Classroom> findByIdAndTeacherId(Integer id, Integer teacherId);

    boolean existsByInviteCode(String inviteCode);

    Optional<Classroom> findByInviteCode(String inviteCode);

    List<Classroom> findByLearningPathId(Integer learningPathId);

    List<Classroom> findByLearningPath_SubjectId(Integer subjectId);
}
