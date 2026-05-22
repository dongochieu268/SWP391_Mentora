package com.edunac.mentora.controller;

import com.edunac.mentora.domain.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/users")
    public String users(HttpSession session, Model model) {
        model.addAttribute("user", (User) session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "users");
        return "admin/users";
    }

    @GetMapping("/create-teacher")
    public String createTeacher(HttpSession session, Model model) {
        model.addAttribute("user", (User) session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "create-teacher");
        return "admin/create-teacher";
    }

    @GetMapping("/students")
    public String students(HttpSession session, Model model) {
        model.addAttribute("user", (User) session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "students");
        return "admin/students";
    }
}
