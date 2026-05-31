package com.edunac.mentora.controller.lecturer;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.domain.classroom.Classroom;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import com.edunac.mentora.service.classroom.ClassroomService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lecturer/classes/{classroomId}/members")
public class LecturerMemberController {

    private final ClassroomMemberService memberService;
    private final ClassroomService classroomService;

    public LecturerMemberController(ClassroomMemberService memberService,
                                    ClassroomService classroomService) {
        this.memberService = memberService;
        this.classroomService = classroomService;
    }

    @GetMapping
    public String list(@PathVariable Integer classroomId,
                       HttpSession session, Model model) {
        User user = currentUser(session);
        Classroom classroom = classroomService.findForTeacher(classroomId, user);
        model.addAttribute("classroom", classroom);
        model.addAttribute("pendingMembers", memberService.getPendingMembers(classroomId));
        model.addAttribute("activeMembers", memberService.getActiveMembers(classroomId));
        model.addAttribute("user", user);
        model.addAttribute("activePage", "classes");
        return "lecturer/class/members";
    }

    @PostMapping("/{memberId}/approve")
    public String approve(@PathVariable Integer classroomId,
                          @PathVariable Integer memberId,
                          HttpSession session, RedirectAttributes ra) {
        verifyTeacher(classroomId, session);
        try {
            memberService.approve(memberId, classroomId);
            ra.addFlashAttribute("success", "Đã chấp nhận yêu cầu tham gia.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + classroomId + "/members";
    }

    @PostMapping("/{memberId}/reject")
    public String reject(@PathVariable Integer classroomId,
                         @PathVariable Integer memberId,
                         HttpSession session, RedirectAttributes ra) {
        verifyTeacher(classroomId, session);
        try {
            memberService.reject(memberId, classroomId);
            ra.addFlashAttribute("success", "Đã từ chối yêu cầu tham gia.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + classroomId + "/members";
    }

    @PostMapping("/{memberId}/assign-ta")
    public String assignTA(@PathVariable Integer classroomId,
                           @PathVariable Integer memberId,
                           HttpSession session, RedirectAttributes ra) {
        verifyTeacher(classroomId, session);
        try {
            memberService.assignTA(memberId, classroomId);
            ra.addFlashAttribute("success", "Đã chỉ định trợ giảng (TA).");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + classroomId + "/members";
    }

    @PostMapping("/{memberId}/revoke-ta")
    public String revokeTA(@PathVariable Integer classroomId,
                           @PathVariable Integer memberId,
                           HttpSession session, RedirectAttributes ra) {
        verifyTeacher(classroomId, session);
        try {
            memberService.revokeTA(memberId, classroomId);
            ra.addFlashAttribute("success", "Đã thu hồi quyền trợ giảng.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/lecturer/classes/" + classroomId + "/members";
    }

    private void verifyTeacher(Integer classroomId, HttpSession session) {
        classroomService.findForTeacher(classroomId, currentUser(session));
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
