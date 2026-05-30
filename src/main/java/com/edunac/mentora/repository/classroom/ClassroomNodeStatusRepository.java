package com.edunac.mentora.repository.classroom;

import com.edunac.mentora.domain.classroom.ClassroomNodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomNodeStatusRepository extends JpaRepository<ClassroomNodeStatus, Integer> {

    List<ClassroomNodeStatus> findByClassroomId(Integer classroomId);
}
