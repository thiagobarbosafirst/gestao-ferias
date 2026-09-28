package com.taskflow.vacations.dto;

import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.model.User;

import java.time.Instant;

/** Representação pública de um utilizador (sem password). */
public record UserResponse(Long id, String name, String email, Role role, UserRef manager, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                UserRef.from(user.getManager()), user.getCreatedAt());
    }
}
