package com.edunac.mentora.repository.level;

import com.edunac.mentora.domain.level.NodeLevelAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface NodeLevelAttemptRepository extends JpaRepository<NodeLevelAttempt, Integer> {

    List<NodeLevelAttempt> findByNodeLevel_IdAndStudent_IdAndClassroom_IdOrderByAttemptNumberDesc(
            Integer nodeLevelId, Integer studentId, Integer classroomId);

    // Resume support: at most one IN_PROGRESS attempt per (level, student, classroom).
    Optional<NodeLevelAttempt> findFirstByNodeLevel_IdAndStudent_IdAndClassroom_IdAndStatusOrderByAttemptNumberDesc(
            Integer nodeLevelId, Integer studentId, Integer classroomId, String status);

    long countByNodeLevel_IdAndStudent_IdAndClassroom_Id(
            Integer nodeLevelId, Integer studentId, Integer classroomId);

    // Blocks level deletion (L3 §5) and node deletion (L1 §3).
    boolean existsByNodeLevel_Id(Integer nodeLevelId);

    boolean existsByNodeLevel_LearningNode_Id(Integer nodeId);

    // Attempt history across all levels of a node (S6, L5).
    List<NodeLevelAttempt> findByNodeLevel_LearningNode_IdAndStudent_IdAndClassroom_IdOrderByNodeLevel_LevelNumberAscAttemptNumberAsc(
            Integer nodeId, Integer studentId, Integer classroomId);

    // Unlock rule (single source of truth, work-plan §3.2):
    // level N+1 is unlocked iff bestSubmittedScore(level N) >= passingScore(level N).
    @Query("""
            select max(attempt.score)
            from NodeLevelAttempt attempt
            where attempt.nodeLevel.id = :nodeLevelId
              and attempt.student.id = :studentId
              and attempt.classroom.id = :classroomId
              and attempt.status = 'SUBMITTED'
            """)
    Optional<BigDecimal> findBestSubmittedScore(
            @Param("nodeLevelId") Integer nodeLevelId,
            @Param("studentId") Integer studentId,
            @Param("classroomId") Integer classroomId);
}
