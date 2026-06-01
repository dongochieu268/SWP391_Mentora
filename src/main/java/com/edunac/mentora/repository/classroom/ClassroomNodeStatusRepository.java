package com.edunac.mentora.repository.classroom;

import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomNodeStatusRepository extends JpaRepository<ClassroomNodeStatus, Integer> {

    List<ClassroomNodeStatus> findByClassroomId(Integer classroomId);

    Optional<ClassroomNodeStatus> findByClassroomIdAndNodeId(Integer classroomId, Integer nodeId);

    void deleteByNodeId(Integer nodeId);
}
