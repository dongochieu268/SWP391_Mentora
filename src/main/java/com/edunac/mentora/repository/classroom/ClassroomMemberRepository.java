package com.edunac.mentora.repository.classroom;

import com.edunac.mentora.domain.classroom.ClassroomMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Integer> {

    List<ClassroomMember> findByClassroomIdAndStatusOrderByJoinedAtAsc(Integer classroomId, String status);

    List<ClassroomMember> findByUserIdAndStatusOrderByJoinedAtDesc(Integer userId, String status);

    boolean existsByClassroomIdAndUserId(Integer classroomId, Integer userId);

    Optional<ClassroomMember> findByIdAndClassroomId(Integer id, Integer classroomId);

    Optional<ClassroomMember> findByClassroomIdAndUserIdAndStatus(
            Integer classroomId, Integer userId, String status);
}
