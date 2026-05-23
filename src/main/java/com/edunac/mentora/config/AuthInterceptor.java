package com.edunac.mentora.config;

import com.edunac.mentora.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

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

        String path = request.getRequestURI();
        String role = user.getRole().getName();

        if (path.startsWith("/admin") && !"ADMIN".equals(role)) {
            response.sendRedirect(dashboardByRole(role));
            return false;
        }
        if (path.startsWith("/lecturer") && !"LECTURER".equals(role)) {
            response.sendRedirect(dashboardByRole(role));
            return false;
        }
        if (path.startsWith("/student") && !"STUDENT".equals(role)) {
            response.sendRedirect(dashboardByRole(role));
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
