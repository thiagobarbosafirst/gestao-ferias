package com.taskflow.vacations.dto;

/** Resposta do login: o token JWT a enviar no header "Authorization: Bearer ..." e os dados do utilizador. */
public record LoginResponse(String token, UserResponse user) {
}
