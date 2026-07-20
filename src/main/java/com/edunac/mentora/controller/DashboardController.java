package com.edunac.mentora.controller;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.service.UserService;
import com.edunac.mentora.service.classroom.ClassroomMemberService;
import com.edunac.mentora.service.classroom.ClassroomService;
import com.edunac.mentora.service.subject.SubjectService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final ClassroomService classroomService;
    private final ClassroomMemberService classroomMemberService;
    private final UserService userService;
    private final SubjectService subjectService;

    public DashboardController(ClassroomService classroomService,
                               ClassroomMemberService classroomMemberService,
                               UserService userService,
                               SubjectService subjectService) {
        this.classroomService = classroomService;
        this.classroomMemberService = classroomMemberService;
        this.userService = userService;
        this.subjectService = subjectService;
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        model.addAttribute("user", (User) session.getAttribute("loggedInUser"));
        model.addAttribute("studentCount", userService.countByRole("STUDENT"));
        model.addAttribute("lecturerCount", userService.countByRole("LECTURER"));
        model.addAttribute("subjectCount", subjectService.countSubjects());
        model.addAttribute("bannedAccountCount", userService.countByStatus("BANNED"));
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/lecturer/dashboard")
    public String lecturerDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("user", user);
        model.addAttribute("expiredClasses", classroomService.findExpiredNeedingCompletion(user));
        model.addAttribute("activePage", "dashboard");
        return "lecturer/dashboard";
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("user", user);
        model.addAttribute("myClassrooms", classroomMemberService.getMyClassrooms(user.getId()));
        model.addAttribute("pendingRequests", classroomMemberService.getMyPendingRequests(user.getId()));
        model.addAttribute("activePage", "dashboard");
        return "student/dashboard";
    }
}
