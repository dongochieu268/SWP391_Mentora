package com.edunac.mentora.repository.classroom;

import com.edunac.mentora.domain.classroom.ClassroomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ClassroomMemberRepository extends JpaRepository<ClassroomMember, Integer> {

    List<ClassroomMember> findByClassroomIdAndStatusOrderByJoinedAtAsc(Integer classroomId, String status);

    List<ClassroomMember> findByUserIdAndStatusOrderByJoinedAtDesc(Integer userId, String status);

    boolean existsByClassroomIdAndUserId(Integer classroomId, Integer userId);

    boolean existsByClassroomId(Integer classroomId);

    boolean existsByClassroomIdAndUserIdAndStatus(Integer classroomId, Integer userId, String status);

    Optional<ClassroomMember> findByIdAndClassroomId(Integer id, Integer classroomId);

    Optional<ClassroomMember> findByClassroomIdAndUserIdAndStatus(
            Integer classroomId, Integer userId, String status);

    List<ClassroomMember> findByClassroomIdOrderByStatusAscJoinedAtAsc(Integer classroomId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ClassroomMember m WHERE m.classroom.id = :classroomId")
    void deleteByClassroomId(Integer classroomId);
}
