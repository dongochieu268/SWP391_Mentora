package com.edunac.mentora.repository.branching;

import com.edunac.mentora.domain.branching.BranchRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRuleRepository extends JpaRepository<BranchRule, Integer> {

    Optional<BranchRule> findByNodeId(Integer nodeId);

    boolean existsByNodeId(Integer nodeId);

    Optional<BranchRule> findByAssessmentId(Integer assessmentId);

    @Query("SELECT r FROM BranchRule r WHERE r.assessmentId = :assessmentId AND r.node.learningPath.id = :pathId")
    Optional<BranchRule> findByAssessmentAndPath(@Param("assessmentId") Integer assessmentId, @Param("pathId") Integer pathId);
}