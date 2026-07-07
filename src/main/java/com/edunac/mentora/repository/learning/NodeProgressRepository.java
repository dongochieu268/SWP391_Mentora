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

    List<NodeProgress> findByClassroom_Id(Integer classroomId);

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
    long countValidCompletedNodes(
            @Param("studentId") Integer studentId,
            @Param("classroomId") Integer classroomId);


    @Query(value = """
        SELECT COUNT(np.id)
        FROM node_progress np
        JOIN classroom_node_status cns
            ON cns.node_id    = np.node_id
           AND cns.classroom_id = np.classroom_id
           AND cns.status     = 'VISIBLE'
        JOIN learning_nodes ln
            ON ln.id = np.node_id
        WHERE np.student_id    = :studentId
          AND np.classroom_id  = :classroomId
          AND np.is_completed  = 1
          AND (
              -- Node MAIN hoặc BRANCH_TEST: luôn tính
              ln.branch_tag IS NULL
              OR ln.branch_tag = 'MAIN'
              OR ln.node_type  = 'BRANCH_TEST'
              OR (
                  -- Node PASS/FAIL: chỉ tính nếu đúng nhánh sinh viên
                  ln.branch_owner_node_id IS NOT NULL
                  AND EXISTS (
                      SELECT 1
                      FROM student_branch_assignments sba
                      WHERE sba.student_id    = :studentId
                        AND sba.classroom_id  = :classroomId
                        AND sba.branch_node_id = ln.branch_owner_node_id
                        AND sba.assigned_branch = ln.branch_tag
                  )
              )
          )
    """, nativeQuery = true)
    long countCompletedNodesForStudent(
            @Param("studentId") Integer studentId,
            @Param("classroomId") Integer classroomId);


    @Query(value = """
        SELECT COUNT(ln.id)
        FROM learning_nodes ln
        JOIN classroom_node_status cns
            ON cns.node_id     = ln.id
           AND cns.classroom_id = :classroomId
           AND cns.status      = 'VISIBLE'
        WHERE (
            ln.branch_tag IS NULL
            OR ln.branch_tag = 'MAIN'
            OR ln.node_type  = 'BRANCH_TEST'
            OR (
                ln.branch_owner_node_id IS NOT NULL
                AND EXISTS (
                    SELECT 1
                    FROM student_branch_assignments sba
                    WHERE sba.student_id    = :studentId
                      AND sba.classroom_id  = :classroomId
                      AND sba.branch_node_id = ln.branch_owner_node_id
                      AND sba.assigned_branch = ln.branch_tag
                )
            )
        )
    """, nativeQuery = true)
    long countRelevantNodesForStudent(
            @Param("studentId") Integer studentId,
            @Param("classroomId") Integer classroomId);
}