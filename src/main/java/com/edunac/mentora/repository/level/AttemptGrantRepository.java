package com.edunac.mentora.repository.level;

import com.edunac.mentora.domain.level.AttemptGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttemptGrantRepository extends JpaRepository<AttemptGrant, Integer> {

    List<AttemptGrant> findByNodeLevel_IdAndStudent_IdAndClassroom_IdOrderByGrantedAtDesc(
            Integer nodeLevelId, Integer studentId, Integer classroomId);

    @Query("""
            select coalesce(sum(g.extraAttempts), 0)
            from AttemptGrant g
            where g.nodeLevel.id = :nodeLevelId
              and g.student.id = :studentId
              and g.classroom.id = :classroomId
            """)
    int sumExtraAttempts(
            @Param("nodeLevelId") Integer nodeLevelId,
            @Param("studentId") Integer studentId,
            @Param("classroomId") Integer classroomId);
}