package com.edunac.mentora.controller;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =====================
    // ROOT — redirect theo role
    // =====================
    @GetMapping("/")
    public String root(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";
        return redirectByRole(user);
    }

    // =====================
    // LOGIN
    // =====================
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return redirectByRole((User) session.getAttribute("loggedInUser"));
        }
        return "auth/authentication-login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        Model model) {
        try {
            User user = authService.login(email, password);
            session.setAttribute("loggedInUser", user);
            return redirectByRole(user);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("email", email);
            return "auth/authentication-login";
        }
    }

    // =====================
    // REGISTER
    // =====================
    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return redirectByRole((User) session.getAttribute("loggedInUser"));
        }
        return "auth/authentication-register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String fullName,
                           @RequestParam String email,
                           @RequestParam String password,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            authService.register(fullName, email, password);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Hãy đăng nhập.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            return "auth/authentication-register";
        }
    }

    // =====================
    // LOGOUT
    // =====================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // =====================
    // HELPER
    // =====================
    private String redirectByRole(User user) {
        return switch (user.getRole().getName()) {
            case "ADMIN" -> "redirect:/admin/dashboard";
            case "LECTURER" -> "redirect:/lecturer/dashboard";
            default -> "redirect:/student/dashboard";
        };
    }
}
