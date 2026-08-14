package com.edunac.mentora.service;

import com.edunac.mentora.domain.Role;
import com.edunac.mentora.domain.User;
import com.edunac.mentora.repository.RoleRepository;
import com.edunac.mentora.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,10}$");
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^[\\p{L} ]+$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public void register(String fullName, String email, String password) {
        validateFullName(fullName);
        validateEmail(email);
        if (userRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }
        validatePassword(password);

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("Role STUDENT không tồn tại trong DB."));

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(studentRole);
        user.setStatus("ACTIVE");
        user.setEmailVerified(false);
        userRepository.save(user);
    }

    public User login(String email, String password) {
        try {
            validateEmail(email);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Email không đúng định dạng.");
        }
        try {
            validatePassword(password);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Mật khẩu không đúng.");
        }

        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng."));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("Tài khoản đã bị khóa hoặc không hoạt động.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng.");
        }
        return user;
    }

    private void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống.");
        }
        String trimmed = fullName.trim();
        if (trimmed.length() < 2 || trimmed.length() > 100) {
            throw new IllegalArgumentException("Họ tên phải có độ dài từ 2 đến 100 ký tự.");
        }
        if (!FULL_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Họ tên chỉ được chứa chữ cái và khoảng trắng.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        String trimmed = email.trim();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Email không đúng định dạng.");
        }
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Email không được vượt quá 255 ký tự.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        if (password.length() < 6 || password.length() > 50) {
            throw new IllegalArgumentException("Mật khẩu phải có độ dài từ 6 đến 50 ký tự.");
        }
        if (password.contains(" ")) {
            throw new IllegalArgumentException("Mật khẩu không được chứa khoảng trắng.");
        }
    }
}
