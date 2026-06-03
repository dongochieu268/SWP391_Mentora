package com.edunac.mentora.config;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public AuthInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("loggedInUser") : null;

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        User fresh = userRepository.findById(user.getId()).orElse(null);
        if (fresh == null || !"ACTIVE".equals(fresh.getStatus())) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String path = request.getServletPath();
        String role = fresh.getRole().getName();
        String ctx = request.getContextPath();

        if (path.startsWith("/admin") && !"ADMIN".equals(role)) {
            response.sendRedirect(ctx + dashboardByRole(role));
            return false;
        }
        if (path.startsWith("/lecturer") && !"LECTURER".equals(role)) {
            response.sendRedirect(ctx + dashboardByRole(role));
            return false;
        }
        if (path.startsWith("/student") && !"STUDENT".equals(role)) {
            response.sendRedirect(ctx + dashboardByRole(role));
            return false;
        }

        return true;
    }

    private String dashboardByRole(String role) {
        return switch (role) {
            case "ADMIN"    -> "/admin/dashboard";
            case "LECTURER" -> "/lecturer/dashboard";
            default         -> "/student/dashboard";
        };
    }
}
