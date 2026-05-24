package com.edunac.mentora.repository.learningpath;

import com.edunac.mentora.domain.learningpath.LearningNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LearningNodeRepository extends JpaRepository<LearningNode, Integer> {

    List<LearningNode> findByLearningPathIdOrderByNodeOrderAsc(Integer learningPathId);

    @Query("""
            SELECT n FROM LearningNode n
            JOIN FETCH n.learningPath p
            LEFT JOIN FETCH p.subject
            ORDER BY p.name, n.nodeOrder
            """)
    List<LearningNode> findAllWithPathOrdered();

    @Query("""
            SELECT n FROM LearningNode n
            JOIN FETCH n.learningPath p
            LEFT JOIN FETCH p.subject
            WHERE n.id = :id
            """)
    Optional<LearningNode> findDetailById(@Param("id") Integer id);
}
