package com.taskflow.vacations.security;

import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.model.User;

/**
 * Utilizador autenticado no pedido HTTP atual.
 * Os controllers recebem-no com {@code @AuthenticationPrincipal AuthenticatedUser actor}.
 */
public record AuthenticatedUser(Long id, String email, String name, Role role) {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
