package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateUserRequest;
import com.att.tdp.issueflow.dto.request.UpdateUserRequest;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.enums.Role;
import com.att.tdp.issueflow.exception.DuplicateResourceException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_success() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("jdoe")
                .email("jdoe@example.com")
                .fullName("John Doe")
                .password("secret")
                .role(Role.DEVELOPER)
                .build();

        when(userRepository.existsByUsername("jdoe")).thenReturn(false);
        when(userRepository.existsByEmail("jdoe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse response = userService.createUser(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("jdoe");
        assertThat(response.getRole()).isEqualTo(Role.DEVELOPER);
    }

    @Test
    void createUser_duplicateUsername_throws() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("jdoe").email("j@e.com").fullName("J").password("p").role(Role.DEVELOPER).build();
        when(userRepository.existsByUsername("jdoe")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");
    }

    @Test
    void createUser_duplicateEmail_throws() {
        CreateUserRequest request = CreateUserRequest.builder()
                .username("jdoe2").email("jdoe@example.com").fullName("J").password("p").role(Role.DEVELOPER).build();
        when(userRepository.existsByUsername("jdoe2")).thenReturn(false);
        when(userRepository.existsByEmail("jdoe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
    }

    @Test
    void getUserById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUser_updatesOnlyProvidedFields() {
        User existing = User.builder().id(1L).username("jdoe").fullName("Old Name").role(Role.DEVELOPER).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any())).thenReturn(existing);

        userService.updateUser(1L, UpdateUserRequest.builder().fullName("New Name").build());

        assertThat(existing.getFullName()).isEqualTo("New Name");
        assertThat(existing.getRole()).isEqualTo(Role.DEVELOPER);
    }

    @Test
    void getAllUsers_returnsList() {
        User u1 = User.builder().id(1L).username("a").email("a@a.com").fullName("A").role(Role.ADMIN).build();
        User u2 = User.builder().id(2L).username("b").email("b@b.com").fullName("B").role(Role.DEVELOPER).build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<UserResponse> result = userService.getAllUsers();
        assertThat(result).hasSize(2);
    }

    @Test
    void deleteUser_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteUser(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUser_self_throws() {
        assertThatThrownBy(() -> userService.deleteUser(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot delete your own account");
    }
}
