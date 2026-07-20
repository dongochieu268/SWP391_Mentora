package com.edunac.mentora.controller.student;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.service.student.StudentRoadmapService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/classrooms")
public class StudentRoadmapController {

    private final StudentRoadmapService roadmapService;

    public StudentRoadmapController(StudentRoadmapService roadmapService) {
        this.roadmapService = roadmapService;
    }

    @GetMapping("/{classroomId}/roadmap")
    public String viewRoadmap(@PathVariable Integer classroomId,
                              HttpSession session,
                              Model model,
                              RedirectAttributes ra) {
        try {
            User user = currentUser(session);
            var roadmap = roadmapService.buildRoadmap(classroomId, user);
            model.addAttribute("roadmap", roadmap);
            model.addAttribute("sidebarRoadmap", roadmap);
            model.addAttribute("classroom", roadmap.getClassroom());
            model.addAttribute("user", user);
            model.addAttribute("activePage", "classrooms");
            return "student/classroom/roadmap";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/student/classrooms";
        }
    }

    private User currentUser(HttpSession session) {
        return (User) session.getAttribute("loggedInUser");
    }
}
