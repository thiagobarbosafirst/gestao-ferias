package com.taskflow.vacations.dto;

import com.taskflow.vacations.model.Role;
import jakarta.validation.constraints.*;

/** Dados para criar um utilizador (apenas ADMIN). */
public record UserCreateRequest(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 100, message = "O nome tem no máximo 100 caracteres") String name,
        @NotBlank(message = "O email é obrigatório") @Email(message = "Email inválido") @Size(max = 150) String email,
        @NotBlank(message = "A password é obrigatória") @Size(min = 6, max = 72, message = "A password deve ter entre 6 e 72 caracteres") String password,
        @NotNull(message = "O role é obrigatório") Role role,
        /* Obrigatório quando role = COLLABORATOR (validado no serviço). */
        Long managerId) {
}
