package com.edunac.mentora.controller.admin;

import com.edunac.mentora.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    // =====================
    // UC06 — Quản lý tài khoản
    // =====================
    @GetMapping("/users")
    public String users(HttpSession session, Model model,
                        @RequestParam(defaultValue = "") String search,
                        @RequestParam(defaultValue = "") String role,
                        @RequestParam(defaultValue = "") String status) {
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "users");
        model.addAttribute("users", userService.getAllFiltered(search, role, status));
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        return "admin/users";
    }

    @PostMapping("/users/{id}/status")
    public String updateUserStatus(@PathVariable Integer id,
                                   @RequestParam String status,
                                   RedirectAttributes ra) {
        try {
            userService.updateStatus(id, status);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // =====================
    // UC07 — Tạo giảng viên
    // =====================
    @GetMapping("/create-teacher")
    public String createTeacherPage(HttpSession session, Model model) {
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "create-teacher");
        return "admin/create-teacher";
    }

    @PostMapping("/create-teacher")
    public String createTeacher(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String password,
                                HttpSession session,
                                Model model,
                                RedirectAttributes ra) {
        try {
            userService.createTeacher(fullName, email, password);
            ra.addFlashAttribute("success", "Tạo tài khoản giảng viên thành công!");
            return "redirect:/admin/create-teacher";
        } catch (Exception e) {
            model.addAttribute("user", session.getAttribute("loggedInUser"));
            model.addAttribute("activePage", "create-teacher");
            model.addAttribute("error", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("teacherEmail", email);
            return "admin/create-teacher";
        }
    }

    // =====================
    // UC08 — Quản lý sinh viên
    // =====================
    @GetMapping("/students")
    public String students(HttpSession session, Model model,
                           @RequestParam(defaultValue = "") String search,
                           @RequestParam(defaultValue = "") String status) {
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "students");
        model.addAttribute("students", userService.getStudentsFiltered(search, status));
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        return "admin/students";
    }

    @PostMapping("/students/{id}/status")
    public String updateStudentStatus(@PathVariable Integer id,
                                      @RequestParam String status,
                                      RedirectAttributes ra) {
        try {
            userService.updateStatus(id, status);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái sinh viên.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/students";
    }
}
