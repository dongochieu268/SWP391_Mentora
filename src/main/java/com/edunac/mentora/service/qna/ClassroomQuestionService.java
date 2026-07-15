package com.edunac.mentora.service.qna;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.domain.classroom.ClassroomMember;
import com.edunac.mentora.domain.classroom.MemberStatus;
import com.edunac.mentora.domain.classroom.MemberRole;
import com.edunac.mentora.domain.qna.ClassroomQuestion;
import com.edunac.mentora.domain.qna.ClassroomQuestionReply;
import com.edunac.mentora.domain.qna.QuestionStatus;
import com.edunac.mentora.repository.qna.ClassroomQuestionReplyRepository;
import com.edunac.mentora.repository.qna.ClassroomQuestionRepository;
import com.edunac.mentora.repository.classroom.ClassroomMemberRepository;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import com.edunac.mentora.service.classroom.ClassroomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ClassroomQuestionService {

    private final ClassroomQuestionRepository questionRepository;
    private final ClassroomQuestionReplyRepository replyRepository;
    private final ClassroomMemberRepository memberRepository;
    private final ClassroomMemberService memberService;
    private final ClassroomService classroomService;

    public ClassroomQuestionService(ClassroomQuestionRepository questionRepository,
                                    ClassroomQuestionReplyRepository replyRepository,
                                    ClassroomMemberRepository memberRepository,
                                    ClassroomMemberService memberService,
                                    ClassroomService classroomService) {
        this.questionRepository = questionRepository;
        this.replyRepository = replyRepository;
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.classroomService = classroomService;
    }

    public List<ClassroomQuestion> findVisibleForStudent(Integer classroomId, User student) {
        memberService.requireActiveMemberRecord(classroomId, student);
        return enrichReplies(classroomId, questionRepository.findByClassroomIdOrderByCreatedAtDesc(classroomId));
    }

    public List<ClassroomQuestion> findVisibleForTa(Integer classroomId, User student) {
        ClassroomMember member = memberService.requireActiveMemberRecord(classroomId, student);
        if (!MemberRole.TA.name().equals(member.getRoleInClass())) {
            throw new IllegalStateException("Ban khong co quyen xem danh sach cau hoi cua lop.");
        }
        return enrichReplies(classroomId, questionRepository.findByClassroomIdOrderByCreatedAtDesc(classroomId));
    }

    public List<ClassroomQuestion> findForLecturer(Integer classroomId, User lecturer) {
        classroomService.findForTeacher(classroomId, lecturer);
        return enrichReplies(classroomId, questionRepository.findByClassroomIdOrderByCreatedAtDesc(classroomId));
    }

    public void ask(Integer classroomId, User student, String content) {
        ClassroomMember member = memberService.requireActiveMemberRecord(classroomId, student);
        String normalized = normalize(content, 1000, "Noi dung cau hoi");

        ClassroomQuestion question = new ClassroomQuestion();
        question.setClassroom(member.getClassroom());
        question.setStudent(student);
        question.setContent(normalized);
        question.setStatus(QuestionStatus.OPEN.name());
        questionRepository.save(question);
    }

    public void answerAsTa(Integer classroomId, Integer questionId, User ta, String answerContent) {
        ClassroomMember member = memberService.requireActiveMemberRecord(classroomId, ta);
        if (!MemberRole.TA.name().equals(member.getRoleInClass())) {
            throw new IllegalStateException("Chi tro giang moi duoc tra loi cau hoi.");
        }
        answer(classroomId, questionId, ta, answerContent, true);
    }

    public void answerAsClassMember(Integer classroomId, Integer questionId, User student, String answerContent) {
        ClassroomMember member = memberService.requireActiveMemberRecord(classroomId, student);
        boolean officialAnswer = MemberRole.TA.name().equals(member.getRoleInClass());
        answer(classroomId, questionId, student, answerContent, officialAnswer);
    }

    public void answerAsLecturer(Integer classroomId, Integer questionId, User lecturer, String answerContent) {
        Classroom classroom = classroomService.findForTeacher(classroomId, lecturer);
        answer(classroom.getId(), questionId, lecturer, answerContent, true);
    }

    public void deleteAsLecturer(Integer classroomId, Integer questionId, User lecturer) {
        Classroom classroom = classroomService.findForTeacher(classroomId, lecturer);
        ClassroomQuestion question = questionRepository.findByIdAndClassroomId(questionId, classroom.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi trong lớp."));
        replyRepository.deleteByQuestionId(question.getId());
        questionRepository.delete(question);
    }

    private void answer(Integer classroomId, Integer questionId, User responder, String answerContent, boolean officialAnswer) {
        ClassroomQuestion question = questionRepository.findByIdAndClassroomId(questionId, classroomId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy câu hỏi trong lớp."));

        ClassroomQuestionReply reply = new ClassroomQuestionReply();
        reply.setQuestion(question);
        reply.setResponder(responder);
        reply.setContent(normalize(answerContent, 2000, "Noi dung tra loi"));
        reply.setOfficial(officialAnswer);
        replyRepository.save(reply);

        if (officialAnswer) {
            question.setAnsweredBy(responder);
            question.setAnsweredAt(LocalDateTime.now());
            question.setStatus(QuestionStatus.ANSWERED.name());
        } else if (replyRepository.existsByQuestionIdAndOfficialTrue(question.getId())) {
            question.setStatus(QuestionStatus.ANSWERED.name());
        } else {
            question.setAnsweredBy(null);
            question.setAnsweredAt(null);
            question.setStatus(QuestionStatus.OPEN.name());
        }
        questionRepository.save(question);
    }

    private List<ClassroomQuestion> enrichReplies(Integer classroomId, List<ClassroomQuestion> questions) {
        for (ClassroomQuestion question : questions) {
            for (ClassroomQuestionReply reply : question.getReplies()) {
                reply.setResponderRoleLabel(resolveResponderRoleLabel(classroomId, reply.getResponder()));
            }
        }
        return questions;
    }

    private String resolveResponderRoleLabel(Integer classroomId, User responder) {
        String accountRole = responder.getRole().getName();
        if ("LECTURER".equals(accountRole)) {
            return "Giảng viên";
        }
        return memberRepository
                .findByClassroomIdAndUserIdAndStatus(classroomId, responder.getId(), MemberStatus.ACTIVE.name())
                .map(member -> MemberRole.TA.name().equals(member.getRoleInClass()) ? "Trợ giảng" : "Sinh viên")
                .orElse("Sinh viên");
    }

    private String normalize(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " khong duoc de trong.");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " toi da " + maxLength + " ky tu.");
        }
        return normalized;
    }
}
