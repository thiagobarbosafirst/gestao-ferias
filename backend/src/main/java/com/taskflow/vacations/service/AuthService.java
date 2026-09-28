package com.taskflow.vacations.service;

import com.taskflow.vacations.dto.LoginRequest;
import com.taskflow.vacations.dto.LoginResponse;
import com.taskflow.vacations.dto.UserResponse;
import com.taskflow.vacations.exception.ApiException;
import com.taskflow.vacations.exception.NotFoundException;
import com.taskflow.vacations.model.User;
import com.taskflow.vacations.repository.UserRepository;
import com.taskflow.vacations.security.AuthenticatedUser;
import com.taskflow.vacations.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login e dados do utilizador autenticado.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Valida email + password e devolve um token JWT.
     * A mensagem de erro é a mesma para email inexistente e password errada,
     * para não revelar que emails existem no sistema.
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Email ou password inválidos"));

        return new LoginResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    /** Devolve os dados do utilizador autenticado. */
    public UserResponse me(AuthenticatedUser actor) {
        return userRepository.findById(actor.id())
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("Utilizador não encontrado"));
    }
}
