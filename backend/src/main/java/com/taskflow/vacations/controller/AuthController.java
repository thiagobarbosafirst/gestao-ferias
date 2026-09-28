package com.taskflow.vacations.controller;

import com.taskflow.vacations.dto.LoginRequest;
import com.taskflow.vacations.dto.LoginResponse;
import com.taskflow.vacations.dto.UserResponse;
import com.taskflow.vacations.security.AuthenticatedUser;
import com.taskflow.vacations.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Autenticação: login (público) e dados do utilizador autenticado.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements // endpoint público: remove o cadeado no Swagger
    @Operation(summary = "Login com email e password; devolve o token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do utilizador autenticado")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser actor) {
        return authService.me(actor);
    }
}
