package com.edunac.mentora.repository.level;
import com.edunac.mentora.domain.level.StudentLevelReview;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudentLevelReviewRepository extends JpaRepository<StudentLevelReview,Integer> {
    boolean existsByNodeLevel_IdAndStudent_IdAndClassroom_Id(Integer levelId,Integer studentId,Integer classroomId);
}
