package com.edunac.mentora.controller.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/classrooms")
public class StudentClassroomController {

    private final ClassroomMemberService memberService;

    public StudentClassroomController(ClassroomMemberService memberService) {
        this.memberService = memberService;
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

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
