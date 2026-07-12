package com.edunac.mentora.repository.learning;

import com.edunac.mentora.domain.learning.NodeContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface NodeContentRepository extends JpaRepository<NodeContent, Integer> {

    List<NodeContent> findByNode_IdOrderByDisplayOrderAscIdAsc(Integer nodeId);

    Optional<NodeContent> findByIdAndNode_Id(Integer id, Integer nodeId);

    boolean existsByNode_Id(Integer nodeId);

    List<NodeContent> findByMaterial_IdOrderByDisplayOrderAscIdAsc(Integer materialId);

    Optional<NodeContent> findByIdAndMaterial_Id(Integer id, Integer materialId);

    boolean existsByMaterial_Id(Integer materialId);

    @Query("""
            SELECT DISTINCT nc.node.id FROM NodeContent nc
            WHERE nc.node.learningPath.id = :pathId
            """)
    Set<Integer> findNodeIdsWithContentByPathId(@Param("pathId") Integer pathId);
}
