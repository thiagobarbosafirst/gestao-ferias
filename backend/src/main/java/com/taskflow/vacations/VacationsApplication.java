package com.taskflow.vacations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Ponto de entrada da aplicação Spring Boot.
 * <p>
 * Excluímos o UserDetailsServiceAutoConfiguration porque a autenticação é feita por JWT
 * (ver SecurityConfig); sem isto o Spring criaria um utilizador "user" com password aleatória.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class VacationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(VacationsApplication.class, args);
    }
}
