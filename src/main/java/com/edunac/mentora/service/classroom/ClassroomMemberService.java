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

    // UC26 - Học sinh tham gia lớp bằng mã mời
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

    // UC27 - Giảng viên xem danh sách thành viên (pending + active)
    public List<ClassroomMember> getPendingMembers(Integer classroomId) {
        return memberRepository.findByClassroomIdAndStatusOrderByJoinedAtAsc(
                classroomId, MemberStatus.PENDING.name());
    }

    public List<ClassroomMember> getActiveMembers(Integer classroomId) {
        return memberRepository.findByClassroomIdAndStatusOrderByJoinedAtAsc(
                classroomId, MemberStatus.ACTIVE.name());
    }

    // UC27 - Giảng viên chấp nhận yêu cầu
    public void approve(Integer memberId, Integer classroomId) {
        ClassroomMember member = findMemberInClass(memberId, classroomId);
        if (!MemberStatus.PENDING.name().equals(member.getStatus())) {
            throw new IllegalArgumentException("Yêu cầu này không ở trạng thái chờ duyệt.");
        }
        member.setStatus(MemberStatus.ACTIVE.name());
        memberRepository.save(member);
    }

    // UC27 - Giảng viên từ chối yêu cầu
    public void reject(Integer memberId, Integer classroomId) {
        ClassroomMember member = findMemberInClass(memberId, classroomId);
        if (!MemberStatus.PENDING.name().equals(member.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể từ chối yêu cầu đang chờ duyệt.");
        }
        memberRepository.delete(member);
    }

    // UC27 - Giảng viên chỉ định trợ giảng (TA)
    public void assignTA(Integer memberId, Integer classroomId) {
        ClassroomMember member = findMemberInClass(memberId, classroomId);
        if (!MemberStatus.ACTIVE.name().equals(member.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể chỉ định trợ giảng cho thành viên đã được duyệt.");
        }
        member.setRoleInClass(MemberRole.TA.name());
        memberRepository.save(member);
    }

    // Đổi TA về lại STUDENT
    public void revokeTA(Integer memberId, Integer classroomId) {
        ClassroomMember member = findMemberInClass(memberId, classroomId);
        member.setRoleInClass(MemberRole.STUDENT.name());
        memberRepository.save(member);
    }

    // Học sinh xem các lớp mình đã tham gia (active)
    public List<ClassroomMember> getMyClassrooms(Integer studentId) {
        return memberRepository.findByUserIdAndStatusOrderByJoinedAtDesc(
                studentId, MemberStatus.ACTIVE.name());
    }

    // Học sinh xem yêu cầu đang chờ
    public List<ClassroomMember> getMyPendingRequests(Integer studentId) {
        return memberRepository.findByUserIdAndStatusOrderByJoinedAtDesc(
                studentId, MemberStatus.PENDING.name());
    }

    // UC33 - Kiểm tra sinh viên đã được duyệt trước khi xem lộ trình
    public Classroom requireActiveMember(Integer classroomId, User student) {
        ClassroomMember member = memberRepository
                .findByClassroomIdAndUserIdAndStatus(
                        classroomId, student.getId(), MemberStatus.ACTIVE.name())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa được duyệt vào lớp hoặc không có quyền xem lộ trình."));
        return member.getClassroom();
    }

    private ClassroomMember findMemberInClass(Integer memberId, Integer classroomId) {
        return memberRepository.findByIdAndClassroomId(memberId, classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thành viên trong lớp."));
    }
}
