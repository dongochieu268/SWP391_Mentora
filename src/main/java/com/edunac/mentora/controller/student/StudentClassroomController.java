package com.edunac.mentora.controller.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.ClassroomMember;
import com.edunac.mentora.domain.classroom.MemberRole;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import com.edunac.mentora.service.qna.ClassroomQuestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/student/classrooms")
public class StudentClassroomController {

    private final ClassroomMemberService memberService;
    private final ClassroomQuestionService questionService;

    public StudentClassroomController(ClassroomMemberService memberService,
                                      ClassroomQuestionService questionService) {
        this.memberService = memberService;
        this.questionService = questionService;
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        User user = currentUser(session);
        model.addAttribute("myClassrooms", memberService.getMyClassrooms(user.getId()));
        model.addAttribute("pendingRequests", memberService.getMyPendingRequests(user.getId()));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classrooms");
        return "student/classroom/list";
    }

    @PostMapping("/join")
    public String join(@RequestParam String inviteCode,
                       HttpSession session,
                       RedirectAttributes ra) {
        try {
            memberService.joinByInviteCode(inviteCode, currentUser(session));
            ra.addFlashAttribute("success", "Đã gửi yêu cầu tham gia lớp. Chờ giảng viên chấp nhận.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/classrooms";
    }

    @GetMapping("/{classroomId}/qna")
    public String qna(@PathVariable Integer classroomId,
                      HttpSession session,
                      Model model,
                      RedirectAttributes ra) {
        User user = currentUser(session);
        try {
            ClassroomMember member = memberService.requireActiveMemberRecord(classroomId, user);
            boolean taMode = MemberRole.TA.name().equals(member.getRoleInClass());
            model.addAttribute("member", member);
            model.addAttribute("classroom", member.getClassroom());
            model.addAttribute("taMode", taMode);
            model.addAttribute("questions", questionService.findVisibleForStudent(classroomId, user));
            model.addAttribute("user", user);
            model.addAttribute("activePage", "classrooms");
            return "student/classroom/qna";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/classrooms";
        }
    }

    @GetMapping("/{classroomId}/role-status")
    @ResponseBody
    public Map<String, Object> roleStatus(@PathVariable Integer classroomId,
                                          HttpSession session) {
        ClassroomMember member = memberService.requireActiveMemberRecord(classroomId, currentUser(session));
        boolean taMode = MemberRole.TA.name().equals(member.getRoleInClass());
        return Map.of(
                "roleInClass", member.getRoleInClass(),
                "taMode", taMode
        );
    }

    @PostMapping("/{classroomId}/qna/questions")
    public String askQuestion(@PathVariable Integer classroomId,
                              @RequestParam String content,
                              HttpSession session,
                              RedirectAttributes ra) {
        try {
            questionService.ask(classroomId, currentUser(session), content);
            ra.addFlashAttribute("success", "Đã gửi câu hỏi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/classrooms/" + classroomId + "/qna";
    }

    @PostMapping("/{classroomId}/qna/questions/{questionId}/answer")
    public String answerQuestion(@PathVariable Integer classroomId,
                                 @PathVariable Integer questionId,
                                 @RequestParam String answerContent,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        try {
            questionService.answerAsClassMember(classroomId, questionId, currentUser(session), answerContent);
            ra.addFlashAttribute("success", "Đã gửi phản hồi.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/classrooms/" + classroomId + "/qna";
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
