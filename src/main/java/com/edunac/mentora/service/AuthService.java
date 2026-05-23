package com.edunac.mentora.service;

import com.edunac.mentora.domain.Role;
import com.edunac.mentora.domain.User;
import com.edunac.mentora.repository.RoleRepository;
import com.edunac.mentora.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public void register(String fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email này đã được sử dụng.");
        }
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("Role STUDENT không tồn tại trong DB."));

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(studentRole);
        user.setStatus("ACTIVE");
        user.setEmailVerified(false);
        userRepository.save(user);
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không đúng."));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new IllegalStateException("Tài khoản đã bị khóa hoặc không hoạt động.");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không đúng.");
        }
        return user;
    }
}
