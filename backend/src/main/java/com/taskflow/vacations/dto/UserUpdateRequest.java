package com.taskflow.vacations.dto;

import com.taskflow.vacations.model.Role;
import jakarta.validation.constraints.*;

/** Dados para editar um utilizador (apenas ADMIN). A password é opcional: se vier vazia mantém-se a atual. */
public record UserUpdateRequest(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 100, message = "O nome tem no máximo 100 caracteres") String name,
        @NotBlank(message = "O email é obrigatório") @Email(message = "Email inválido") @Size(max = 150) String email,
        @Size(min = 6, max = 72, message = "A password deve ter entre 6 e 72 caracteres") String password,
        @NotNull(message = "O role é obrigatório") Role role,
        Long managerId) {
}
