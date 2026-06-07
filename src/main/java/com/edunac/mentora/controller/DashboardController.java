package com.edunac.mentora.controller;

import com.edunac.mentora.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        model.addAttribute("user", (User) session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "dashboard");
        return "admin/dashboard";
    }

    @GetMapping("/lecturer/dashboard")
    public String lecturerDashboard(HttpSession session, Model model) {
        model.addAttribute("user", (User) session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "dashboard");
        return "lecturer/dashboard";
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard(HttpSession session, Model model) {
        model.addAttribute("user", (User) session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "dashboard");
        return "student/dashboard";
    }
}
