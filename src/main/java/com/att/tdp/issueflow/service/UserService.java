package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateUserRequest;
import com.att.tdp.issueflow.dto.request.UpdateUserRequest;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.exception.DuplicateResourceException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.mapper.UserMapper;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = findUserOrThrow(id);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = UserMapper.toEntity(request, encodedPassword);
        User saved = userRepository.save(user);

        auditLogService.logUserAction(
                com.att.tdp.issueflow.enums.AuditAction.CREATE,
                com.att.tdp.issueflow.enums.EntityType.USER,
                saved.getId(),
                saved.getId()
        );

        return UserMapper.toResponse(saved);
    }

    @Transactional
    public void updateUser(Long id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        userRepository.save(user);

        auditLogService.logUserAction(
                com.att.tdp.issueflow.enums.AuditAction.UPDATE,
                com.att.tdp.issueflow.enums.EntityType.USER,
                id,
                id
        );
    }

    @Transactional
    public void deleteUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("Cannot delete your own account");
        }

        User user = findUserOrThrow(id);
        userRepository.delete(user);

        auditLogService.logUserAction(
                com.att.tdp.issueflow.enums.AuditAction.DELETE,
                com.att.tdp.issueflow.enums.EntityType.USER,
                id,
                currentUserId
        );
    }

    public User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
