package com.taskflow.vacations.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corpo do pedido de login. */
public record LoginRequest(
        @NotBlank(message = "O email é obrigatório") @Email(message = "Email inválido") String email,
        @NotBlank(message = "A password é obrigatória") String password) {
}
