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
    // Quản lý tài khoản gộp (accounts page)
    // =====================
    @GetMapping("/accounts")
    public String accounts(HttpSession session, Model model,
                           @RequestParam(defaultValue = "students") String tab,
                           @RequestParam(defaultValue = "") String search,
                           @RequestParam(defaultValue = "") String status) {
        model.addAttribute("user", session.getAttribute("loggedInUser"));
        model.addAttribute("activePage", "accounts");
        model.addAttribute("activeTab", tab);
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        if ("lecturers".equals(tab)) {
            model.addAttribute("accounts", userService.getAllFiltered(search, "LECTURER", status));
        } else {
            model.addAttribute("accounts", userService.getStudentsFiltered(search, status));
        }
        return "admin/accounts";
    }

    @PostMapping("/accounts/{id}/status")
    public String updateAccountStatus(@PathVariable Integer id,
                                      @RequestParam String status,
                                      @RequestParam(defaultValue = "students") String tab,
                                      RedirectAttributes ra) {
        try {
            userService.updateStatus(id, status);
            ra.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/accounts?tab=" + tab;
    }

    // =====================
    // UC06 — Quản lý tài khoản (legacy redirects)
    // =====================
    @GetMapping("/users")
    public String users() {
        return "redirect:/admin/accounts?tab=lecturers";
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
        return "redirect:/admin/accounts?tab=lecturers";
    }

    // =====================
    // UC07 — Tạo giảng viên
    // =====================
    @GetMapping("/create-teacher")
    public String createTeacherPage() {
        return "redirect:/admin/accounts?tab=lecturers";
    }

    @PostMapping("/create-teacher")
    public String createTeacher(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam String password,
                                RedirectAttributes ra) {
        try {
            userService.createTeacher(fullName, email, password);
            ra.addFlashAttribute("success", "Tạo tài khoản giảng viên thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("openCreateTeacher", true);
            ra.addFlashAttribute("fullName", fullName);
            ra.addFlashAttribute("teacherEmail", email);
        }
        return "redirect:/admin/accounts?tab=lecturers";
    }

    // =====================
    // UC08 — Quản lý sinh viên (legacy redirect)
    // =====================
    @GetMapping("/students")
    public String students() {
        return "redirect:/admin/accounts?tab=students";
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
        return "redirect:/admin/accounts?tab=students";
    }
}
