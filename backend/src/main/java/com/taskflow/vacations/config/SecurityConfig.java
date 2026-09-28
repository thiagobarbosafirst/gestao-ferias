package com.taskflow.vacations.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.vacations.exception.ErrorResponse;
import com.taskflow.vacations.repository.UserRepository;
import com.taskflow.vacations.security.JwtAuthenticationFilter;
import com.taskflow.vacations.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Configuração de segurança.
 * <ul>
 *   <li>API stateless: sem sessão/cookies, cada pedido traz o token JWT.</li>
 *   <li>Endpoints públicos: login e Swagger. Todos os outros exigem token.</li>
 *   <li>{@code @EnableMethodSecurity} ativa o {@code @PreAuthorize} usado nos controllers
 *       (ex.: só ADMIN gere utilizadores).</li>
 *   <li>Erros 401/403 devolvidos no mesmo formato JSON do resto da API.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   UserRepository userRepository, ObjectMapper objectMapper)
            throws Exception {
        http
                // CSRF só é relevante com cookies de sessão; com JWT no header não se aplica.
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        // Sem token ou token inválido
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(objectMapper, req, res, HttpStatus.UNAUTHORIZED,
                                        "Autenticação necessária. Faça login e envie o token."))
                        // Autenticado mas sem permissão
                        .accessDeniedHandler((req, res, e) ->
                                writeError(objectMapper, req, res, HttpStatus.FORBIDDEN,
                                        "Não tem permissão para realizar esta operação")))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** BCrypt: algoritmo de hash de passwords com "salt" automático. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeError(ObjectMapper mapper, HttpServletRequest req, HttpServletResponse res,
                                   HttpStatus status, String message) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                message, req.getRequestURI(), List.of());
        mapper.writeValue(res.getOutputStream(), body);
    }
}
