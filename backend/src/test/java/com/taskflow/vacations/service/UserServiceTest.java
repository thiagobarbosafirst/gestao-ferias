package com.taskflow.vacations.service;

import com.taskflow.vacations.dto.UserCreateRequest;
import com.taskflow.vacations.exception.BusinessException;
import com.taskflow.vacations.exception.ConflictException;
import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.model.User;
import com.taskflow.vacations.repository.UserRepository;
import com.taskflow.vacations.repository.VacationRequestRepository;
import com.taskflow.vacations.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Testes unitários das regras de gestão de colaboradores. */
class UserServiceTest {

    private UserRepository userRepo;
    private VacationRequestRepository vacationRepo;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        vacationRepo = mock(VacationRequestRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenReturn("hashed");
        when(userRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new UserService(userRepo, vacationRepo, encoder);
    }

    @Test
    void collaboratorRequiresManager() {
        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("Joana", "joana@x.com", "secret1", Role.COLLABORATOR, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("manager");
    }

    @Test
    void managerMustHaveManagerRole() {
        User notAManager = user(9L, Role.COLLABORATOR);
        when(userRepo.findById(9L)).thenReturn(Optional.of(notAManager));

        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("Joana", "joana@x.com", "secret1", Role.COLLABORATOR, 9L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createsCollaboratorWithManagerAndHashedPassword() {
        User manager = user(2L, Role.MANAGER);
        when(userRepo.findById(2L)).thenReturn(Optional.of(manager));

        var response = service.create(new UserCreateRequest("Joana", " Joana@X.com ", "secret1", Role.COLLABORATOR, 2L));

        assertThat(response.manager().id()).isEqualTo(2L);
        assertThat(response.email()).isEqualTo("joana@x.com");
        verify(userRepo).save(argThat(u -> u.getPassword().equals("hashed")));
    }

    @Test
    void duplicateEmailIsConflict() {
        when(userRepo.existsByEmailIgnoreCase("joana@x.com")).thenReturn(true);
        assertThatThrownBy(() -> service.create(
                new UserCreateRequest("Joana", "joana@x.com", "secret1", Role.ADMIN, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cannotDeleteManagerWithTeam() {
        User manager = user(2L, Role.MANAGER);
        when(userRepo.findById(2L)).thenReturn(Optional.of(manager));
        when(userRepo.existsByManagerId(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(2L, new AuthenticatedUser(1L, "a@x.com", "Admin", Role.ADMIN)))
                .isInstanceOf(ConflictException.class);
        verify(userRepo, never()).delete(any());
    }

    @Test
    void cannotDeleteSelf() {
        User admin = user(1L, Role.ADMIN);
        when(userRepo.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.delete(1L, AuthenticatedUser.from(admin)))
                .isInstanceOf(BusinessException.class);
    }

    private static User user(Long id, Role role) {
        User u = new User("User " + id, "user" + id + "@x.com", "hash", role, null);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}
