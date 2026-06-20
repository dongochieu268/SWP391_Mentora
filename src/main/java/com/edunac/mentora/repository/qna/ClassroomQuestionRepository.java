package com.edunac.mentora.repository.qna;

import com.edunac.mentora.domain.qna.ClassroomQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomQuestionRepository extends JpaRepository<ClassroomQuestion, Integer> {

    List<ClassroomQuestion> findByClassroomIdOrderByCreatedAtDesc(Integer classroomId);

    List<ClassroomQuestion> findByClassroomIdAndStudentIdOrderByCreatedAtDesc(Integer classroomId, Integer studentId);

    Optional<ClassroomQuestion> findByIdAndClassroomId(Integer id, Integer classroomId);
}
