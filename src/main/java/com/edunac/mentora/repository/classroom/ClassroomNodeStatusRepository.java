package com.edunac.mentora.repository.classroom;

import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ClassroomNodeStatusRepository extends JpaRepository<ClassroomNodeStatus, Integer> {

    List<ClassroomNodeStatus> findByClassroomId(Integer classroomId);

    Optional<ClassroomNodeStatus> findByClassroomIdAndNodeId(Integer classroomId, Integer nodeId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClassroomNodeStatus s WHERE s.node.id = :nodeId")
    void deleteByNodeId(Integer nodeId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClassroomNodeStatus s WHERE s.classroom.id = :classroomId")
    void deleteByClassroomId(Integer classroomId);
}