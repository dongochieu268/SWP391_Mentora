package com.edunac.mentora.repository.learning;

import com.edunac.mentora.domain.learning.NodeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NodeProgressRepository extends JpaRepository<NodeProgress, Integer> {

    List<NodeProgress> findByClassroomIdAndStudentId(Integer classroomId, Integer studentId);

    boolean existsByLearningNodeId(Integer nodeId);

    boolean existsByStudentIdAndLearningNodeIdAndClassroomIdAndCompletedTrue(Integer studentId, Integer learningNodeId, Integer classroomId);

    @Query("""
        SELECT COUNT(np) FROM NodeProgress np 
        WHERE np.student.id = :studentId 
        AND np.classroom.id = :classroomId 
        AND np.completed = true 
        AND EXISTS (
            SELECT 1 FROM ClassroomNodeStatus cns 
            WHERE cns.classroom.id = np.classroom.id 
            AND cns.node.id = np.learningNode.id 
            AND cns.status = 'VISIBLE'
        )
    """)
    long countValidCompletedNodes(@Param("studentId") Integer studentId, @Param("classroomId") Integer classroomId);
}