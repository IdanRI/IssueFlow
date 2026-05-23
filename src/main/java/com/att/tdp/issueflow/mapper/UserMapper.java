package com.att.tdp.issueflow.mapper;

import com.att.tdp.issueflow.dto.request.CreateUserRequest;
import com.att.tdp.issueflow.dto.response.UserResponse;
import com.att.tdp.issueflow.entity.User;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    public static User toEntity(CreateUserRequest request, String encodedPassword) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(encodedPassword)
                .role(request.getRole())
                .build();
    }
}
