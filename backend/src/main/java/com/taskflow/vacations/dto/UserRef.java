package com.taskflow.vacations.dto;

import com.taskflow.vacations.model.User;

/** Referência curta a um utilizador (id + nome), usada dentro de outras respostas. */
public record UserRef(Long id, String name) {

    public static UserRef from(User user) {
        return user == null ? null : new UserRef(user.getId(), user.getName());
    }
}
