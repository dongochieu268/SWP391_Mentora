package com.edunac.mentora.service.classroom;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.*;
import com.edunac.mentora.repository.classroom.ClassroomMemberRepository;
import com.edunac.mentora.repository.classroom.ClassroomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ClassroomMemberService {

    private final ClassroomMemberRepository memberRepository;
    private final ClassroomRepository classroomRepository;

    public ClassroomMemberService(
            ClassroomMemberRepository memberRepository,
            ClassroomRepository classroomRepository
    ) {
        this.memberRepository = memberRepository;
        this.classroomRepository = classroomRepository;
    }

    public void joinByInviteCode(String inviteCode, User student) {
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã lớp.");
        }
        Classroom classroom = classroomRepository.findByInviteCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Mã lớp không hợp lệ."));

        if (!ClassroomStatus.OPEN.name().equals(classroom.getStatus())) {
            throw new IllegalArgumentException("Lớp học này đã đóng, không thể tham gia.");
        }
        if (memberRepository.existsByClassroomIdAndUserId(classroom.getId(), student.getId())) {
            throw new IllegalArgumentException("Bạn đã gửi yêu cầu hoặc đã là thành viên lớp này rồi.");
        }

        ClassroomMember member = new ClassroomMember();
        member.setClassroom(classroom);
        member.setUser(student);
        member.setRoleInClass(MemberRole.STUDENT.name());
        member.setStatus(MemberStatus.PENDING.name());
        memberRepository.save(member);
    }

    public List<ClassroomMember> getMyClassrooms(Integer studentId) {
        return memberRepository.findByUserIdAndStatusOrderByJoinedAtDesc(
                studentId, MemberStatus.ACTIVE.name());
    }

    public List<ClassroomMember> getMyPendingRequests(Integer studentId) {
        return memberRepository.findByUserIdAndStatusOrderByJoinedAtDesc(
                studentId, MemberStatus.PENDING.name());
    }

    public Classroom requireActiveMember(Integer classroomId, User student) {
        ClassroomMember member = memberRepository
                .findByClassroomIdAndUserIdAndStatus(
                        classroomId, student.getId(), MemberStatus.ACTIVE.name())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa được duyệt vào lớp hoặc không có quyền xem lộ trình."));
        return member.getClassroom();
    }
}
