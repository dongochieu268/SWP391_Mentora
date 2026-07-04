package com.edunac.mentora.repository.level;

import com.edunac.mentora.domain.level.NodeLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeLevelRepository extends JpaRepository<NodeLevel, Integer> {

    List<NodeLevel> findByLearningNode_IdOrderByLevelNumberAsc(Integer nodeId);

    List<NodeLevel> findByLearningNode_IdInOrderByLevelNumberAsc(List<Integer> nodeIds);

    Optional<NodeLevel> findByLearningNode_IdAndLevelNumber(Integer nodeId, Integer levelNumber);

    boolean existsByLearningNode_IdAndLevelNumber(Integer nodeId, Integer levelNumber);

    long countByLearningNode_Id(Integer nodeId);

    void deleteByLearningNode_Id(Integer nodeId);
}
