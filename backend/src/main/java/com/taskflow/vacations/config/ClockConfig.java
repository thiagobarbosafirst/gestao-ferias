package com.taskflow.vacations.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Relógio da aplicação. Os serviços usam {@code LocalDate.now(clock)} em vez de {@code LocalDate.now()}
 * para que os testes possam fixar a "data de hoje".
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
