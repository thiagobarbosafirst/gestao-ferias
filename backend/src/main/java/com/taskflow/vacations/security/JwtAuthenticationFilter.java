package com.taskflow.vacations.security;

import com.taskflow.vacations.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro executado uma vez por pedido HTTP:
 * <ol>
 *   <li>lê o header {@code Authorization: Bearer <token>};</li>
 *   <li>valida o token e obtém o id do utilizador;</li>
 *   <li>carrega o utilizador da base de dados (assim, se foi removido ou mudou de role, isso aplica-se logo);</li>
 *   <li>coloca-o no SecurityContext com a authority {@code ROLE_<ROLE>}.</li>
 * </ol>
 * Se não houver token (ou for inválido) o pedido continua sem autenticação e o Spring Security
 * devolve 401 nos endpoints protegidos.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            jwtService.extractUserId(token)
                    .flatMap(userRepository::findById)
                    .ifPresent(user -> {
                        AuthenticatedUser principal = AuthenticatedUser.from(user);
                        var authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
                        var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }

        chain.doFilter(request, response);
    }
}
