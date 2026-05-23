package com.att.tdp.issueflow.config;

import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.enums.Role;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfNotExists("admin", "admin@issueflow.com", "System Admin", "admin123", Role.ADMIN);
        createUserIfNotExists("dev1", "dev1@issueflow.com", "Alice Developer", "dev123", Role.DEVELOPER);
        createUserIfNotExists("dev2", "dev2@issueflow.com", "Bob Developer", "dev123", Role.DEVELOPER);
    }

    private void createUserIfNotExists(String username, String email, String fullName,
                                       String password, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .fullName(fullName)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .build();
            userRepository.save(user);
            log.info("Created seed user: {} ({})", username, role);
        }
    }
}
