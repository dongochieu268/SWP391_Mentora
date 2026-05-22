package com.edunac.mentora.config;

import com.edunac.mentora.domain.Role;
import com.edunac.mentora.domain.User;
import com.edunac.mentora.repository.RoleRepository;
import com.edunac.mentora.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataInitializer(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        createAdminIfNotExists("admin1@mentora.com", "admin123", "Admin");
    }

    private void createAdminIfNotExists(String email, String rawPassword, String fullName) {
        if (userRepository.existsByEmail(email)) return;

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException("Role ADMIN không tồn tại. Hãy chạy SQLQuery1.sql trước."));

        User admin = new User();
        admin.setFullName(fullName);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(adminRole);
        admin.setStatus("ACTIVE");
        admin.setEmailVerified(true);
        userRepository.save(admin);

        System.out.println(">>> Admin account created: " + email + " / " + rawPassword);
    }
}
