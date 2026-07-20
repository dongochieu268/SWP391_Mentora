package com.edunac.mentora.service;

import com.edunac.mentora.domain.User;
import com.edunac.mentora.repository.RoleRepository;
import com.edunac.mentora.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    // UC06 — lấy tất cả user có filter
    public List<User> getAllFiltered(String search, String role, String status) {
        return userRepository.findAllFiltered(
                nullIfEmpty(role),
                nullIfEmpty(status),
                nullIfEmpty(search));
    }

    // UC07 — tạo giảng viên
    public void createTeacher(String fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }
        var lecturerRole = roleRepository.findByName("LECTURER")
                .orElseThrow(() -> new IllegalStateException(
                        "Role LECTURER không tồn tại. Chạy: UPDATE roles SET name='LECTURER' WHERE name='TEACHER'"));

        User teacher = new User();
        teacher.setFullName(fullName);
        teacher.setEmail(email);
        teacher.setPassword(passwordEncoder.encode(password));
        teacher.setRole(lecturerRole);
        teacher.setStatus("ACTIVE");
        teacher.setEmailVerified(true);
        userRepository.save(teacher);
    }

    // UC06 & UC08 — đổi trạng thái
    public void updateStatus(Integer userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản."));
        if ("ADMIN".equals(user.getRole().getName())) {
            throw new IllegalStateException("Không thể thay đổi trạng thái tài khoản Admin.");
        }
        user.setStatus(status);
        userRepository.save(user);
    }

    // UC08 — lấy sinh viên có filter
    public List<User> getStudentsFiltered(String search, String status) {
        return userRepository.findStudentsFiltered(
                nullIfEmpty(status),
                nullIfEmpty(search));
    }

    public long countByRole(String roleName) {
        return userRepository.countByRole_Name(roleName);
    }

    public long countByStatus(String status) {
        return userRepository.countByStatus(status);
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
