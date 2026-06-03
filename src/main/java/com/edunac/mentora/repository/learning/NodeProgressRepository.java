package com.edunac.mentora.repository.learning;

import com.edunac.mentora.domain.learning.NodeProgress;
import org.springframework.data.jpa.repository.JpaRepository;
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
}