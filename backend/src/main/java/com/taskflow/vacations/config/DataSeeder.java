package com.taskflow.vacations.config;

import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.model.User;
import com.taskflow.vacations.model.VacationRequest;
import com.taskflow.vacations.repository.UserRepository;
import com.taskflow.vacations.repository.VacationRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Dados de exemplo criados no arranque, APENAS se a base de dados estiver vazia.
 * Desativar com a variável de ambiente {@code SEED_DATA=false}.
 * <p>
 * Todos os utilizadores têm a password {@value #DEFAULT_PASSWORD}.
 * As datas das férias são relativas a "hoje" para a demo ter sempre dados no futuro.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    public static final String DEFAULT_PASSWORD = "password123";
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final VacationRequestRepository vacationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(UserRepository userRepository, VacationRequestRepository vacationRepository,
                      PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.vacationRepository = vacationRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Base de dados vazia: a criar dados de exemplo (password de todos: {})", DEFAULT_PASSWORD);

        user("Ana Admin", "admin@taskflow.com", Role.ADMIN, null);
        User marco = user("Marco Manager", "marco@taskflow.com", Role.MANAGER, null);
        User sofia = user("Sofia Santos", "sofia@taskflow.com", Role.MANAGER, null);
        User joao = user("João Silva", "joao@taskflow.com", Role.COLLABORATOR, marco);
        User maria = user("Maria Costa", "maria@taskflow.com", Role.COLLABORATOR, marco);
        User pedro = user("Pedro Alves", "pedro@taskflow.com", Role.COLLABORATOR, sofia);
        User rita = user("Rita Lopes", "rita@taskflow.com", Role.COLLABORATOR, sofia);

        LocalDate today = LocalDate.now(clock);

        VacationRequest joaoApproved = vacation(joao, today.plusDays(10), today.plusDays(14), "Viagem em família");
        joaoApproved.approve(marco);

        vacation(maria, today.plusDays(20), today.plusDays(24), null);
        vacation(pedro, today.plusDays(30), today.plusDays(34), "Casamento de um amigo");

        // Um pedido rejeitado não ocupa dias: o período de Rita (dias 40-44) fica livre.
        VacationRequest ritaRejected = vacation(rita, today.plusDays(40), today.plusDays(44), null);
        ritaRejected.reject(sofia, "Semana de entrega do projeto");

        VacationRequest ritaApproved = vacation(rita, today.plusDays(50), today.plusDays(59), null);
        ritaApproved.approve(sofia);
    }

    private User user(String name, String email, Role role, User manager) {
        return userRepository.save(new User(name, email, passwordEncoder.encode(DEFAULT_PASSWORD), role, manager));
    }

    private VacationRequest vacation(User user, LocalDate start, LocalDate end, String notes) {
        return vacationRepository.save(new VacationRequest(user, start, end, notes));
    }
}
