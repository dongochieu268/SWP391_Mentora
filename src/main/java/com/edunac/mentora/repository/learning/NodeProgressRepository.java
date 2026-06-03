package com.edunac.mentora.repository.learning;

import com.edunac.mentora.domain.learning.NodeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NodeProgressRepository extends JpaRepository<NodeProgress, Integer> {

    List<NodeProgress> findByClassroom_IdAndStudent_Id(Integer classroomId, Integer studentId);

    Optional<NodeProgress> findByStudent_IdAndLearningNode_IdAndClassroom_Id(
            Integer studentId, Integer nodeId, Integer classroomId);

    List<NodeProgress> findByStudent_IdAndClassroom_Id(Integer studentId, Integer classroomId);

    long countByStudent_IdAndClassroom_IdAndCompletedTrue(Integer studentId, Integer classroomId);

    boolean existsByStudent_IdAndLearningNode_IdAndClassroom_IdAndCompletedTrue(
            Integer studentId, Integer nodeId, Integer classroomId);

    boolean existsByLearningNodeId(Integer nodeId);

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
