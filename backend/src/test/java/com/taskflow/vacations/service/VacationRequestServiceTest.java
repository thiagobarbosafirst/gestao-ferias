package com.taskflow.vacations.service;

import com.taskflow.vacations.dto.VacationRequestCreate;
import com.taskflow.vacations.dto.VacationRequestResponse;
import com.taskflow.vacations.dto.VacationRequestUpdate;
import com.taskflow.vacations.exception.BusinessException;
import com.taskflow.vacations.exception.ConflictException;
import com.taskflow.vacations.exception.ForbiddenException;
import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.model.User;
import com.taskflow.vacations.model.VacationRequest;
import com.taskflow.vacations.model.VacationStatus;
import com.taskflow.vacations.repository.UserRepository;
import com.taskflow.vacations.repository.VacationRequestRepository;
import com.taskflow.vacations.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários das regras de negócio dos pedidos de férias.
 * Os repositórios são "mocks" (Mockito), por isso não é preciso base de dados.
 */
class VacationRequestServiceTest {

    /** "Hoje" fixo nos testes. */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 1);

    private VacationRequestRepository vacationRepo;
    private UserRepository userRepo;
    private VacationRequestService service;

    private User admin;
    private User manager;
    private User otherManager;
    private User collaborator;
    private User otherCollaborator;

    @BeforeEach
    void setUp() {
        vacationRepo = mock(VacationRequestRepository.class);
        userRepo = mock(UserRepository.class);
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new VacationRequestService(vacationRepo, userRepo, clock);

        admin = user(1L, "Admin", Role.ADMIN, null);
        manager = user(2L, "Manager", Role.MANAGER, null);
        otherManager = user(3L, "Other Manager", Role.MANAGER, null);
        collaborator = user(4L, "Colab", Role.COLLABORATOR, manager);
        otherCollaborator = user(5L, "Other Colab", Role.COLLABORATOR, otherManager);

        // Por omissão: não há sobreposições e o save devolve o próprio objeto.
        when(vacationRepo.findOverlapping(any(), any(), any(), anyLong())).thenReturn(List.of());
        when(vacationRepo.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------ criação

    @Test
    void collaboratorCreatesOwnRequestAsPending() {
        VacationRequestResponse response = service.create(auth(collaborator),
                new VacationRequestCreate(date(8, 1), date(8, 5), "praia", null));

        assertThat(response.status()).isEqualTo(VacationStatus.PENDENTE);
        assertThat(response.employee().id()).isEqualTo(collaborator.getId());
        assertThat(response.days()).as("RB-002: datas inclusivas").isEqualTo(5);
    }

    @Test
    void singleDayRequestCountsAsOneDay() {
        VacationRequestResponse response = service.create(auth(collaborator),
                new VacationRequestCreate(date(8, 1), date(8, 1), null, null));
        assertThat(response.days()).isEqualTo(1);
    }

    @Test
    void rejectsOverlappingPeriod() {
        VacationRequest existing = request(10L, otherCollaborator, date(8, 1), date(8, 5), VacationStatus.APROVADO);
        when(vacationRepo.findOverlapping(eq(date(8, 5)), eq(date(8, 10)), eq(VacationStatus.BLOCKING), eq(-1L)))
                .thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(auth(collaborator),
                new VacationRequestCreate(date(8, 5), date(8, 10), null, null)))
                .isInstanceOf(ConflictException.class)
                // o colaborador não pode ver pedidos de outros -> nome não é revelado
                .hasMessageContaining("outro colaborador")
                .hasMessageNotContaining("Other Colab");
        verify(vacationRepo, never()).saveAndFlush(any());
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> service.create(auth(collaborator),
                new VacationRequestCreate(date(8, 10), date(8, 5), null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsDatesInThePast() {
        assertThatThrownBy(() -> service.create(auth(collaborator),
                new VacationRequestCreate(TODAY.minusDays(1), TODAY.plusDays(2), null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void collaboratorCannotCreateForSomeoneElse() {
        when(userRepo.findById(otherCollaborator.getId())).thenReturn(Optional.of(otherCollaborator));
        assertThatThrownBy(() -> service.create(auth(collaborator),
                new VacationRequestCreate(date(8, 1), date(8, 5), null, otherCollaborator.getId())))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void managerCanCreateForOwnTeamOnly() {
        when(userRepo.findById(collaborator.getId())).thenReturn(Optional.of(collaborator));
        when(userRepo.findById(otherCollaborator.getId())).thenReturn(Optional.of(otherCollaborator));

        assertThat(service.create(auth(manager),
                new VacationRequestCreate(date(8, 1), date(8, 5), null, collaborator.getId())).employee().id())
                .isEqualTo(collaborator.getId());

        assertThatThrownBy(() -> service.create(auth(manager),
                new VacationRequestCreate(date(9, 1), date(9, 5), null, otherCollaborator.getId())))
                .isInstanceOf(ForbiddenException.class);
    }

    // ------------------------------------------------------------ edição / cancelamento

    @Test
    void updateExcludesItselfFromOverlapCheck() {
        VacationRequest own = request(20L, collaborator, date(8, 1), date(8, 5), VacationStatus.PENDENTE);
        when(vacationRepo.findById(20L)).thenReturn(Optional.of(own));

        service.update(auth(collaborator), 20L, new VacationRequestUpdate(date(8, 2), date(8, 6), null));

        verify(vacationRepo).findOverlapping(date(8, 2), date(8, 6), VacationStatus.BLOCKING, 20L);
        assertThat(own.getStartDate()).isEqualTo(date(8, 2));
    }

    @Test
    void cannotEditApprovedRequest() {
        VacationRequest approved = request(21L, collaborator, date(8, 1), date(8, 5), VacationStatus.APROVADO);
        when(vacationRepo.findById(21L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.update(auth(collaborator), 21L,
                new VacationRequestUpdate(date(8, 2), date(8, 6), null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void collaboratorCannotCancelSomeoneElsesRequest() {
        VacationRequest other = request(22L, otherCollaborator, date(8, 1), date(8, 5), VacationStatus.PENDENTE);
        when(vacationRepo.findById(22L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.cancel(auth(collaborator), 22L)).isInstanceOf(ForbiddenException.class);
        verify(vacationRepo, never()).delete(any(VacationRequest.class));
    }

    @Test
    void collaboratorCancelsOwnPendingRequest() {
        VacationRequest own = request(23L, collaborator, date(8, 1), date(8, 5), VacationStatus.PENDENTE);
        when(vacationRepo.findById(23L)).thenReturn(Optional.of(own));

        service.cancel(auth(collaborator), 23L);
        verify(vacationRepo).delete(own);
    }

    // ------------------------------------------------------------ aprovação / rejeição

    @Test
    void managerApprovesOwnTeamRequest() {
        VacationRequest pending = request(30L, collaborator, date(8, 1), date(8, 5), VacationStatus.PENDENTE);
        when(vacationRepo.findById(30L)).thenReturn(Optional.of(pending));

        VacationRequestResponse response = service.approve(auth(manager), 30L);

        assertThat(response.status()).isEqualTo(VacationStatus.APROVADO);
        assertThat(response.decidedBy().id()).isEqualTo(manager.getId());
    }

    @Test
    void managerCannotApproveOtherTeamRequest() {
        VacationRequest pending = request(31L, otherCollaborator, date(8, 1), date(8, 5), VacationStatus.PENDENTE);
        when(vacationRepo.findById(31L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.approve(auth(manager), 31L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void collaboratorCannotApprove() {
        VacationRequest pending = request(32L, collaborator, date(8, 1), date(8, 5), VacationStatus.PENDENTE);
        when(vacationRepo.findById(32L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.approve(auth(collaborator), 32L)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminCanRejectAnyRequest() {
        VacationRequest pending = request(33L, otherCollaborator, date(8, 1), date(8, 5), VacationStatus.PENDENTE);
        when(vacationRepo.findById(33L)).thenReturn(Optional.of(pending));

        VacationRequestResponse response = service.reject(auth(admin), 33L, "Período crítico");

        assertThat(response.status()).isEqualTo(VacationStatus.REJEITADO);
        assertThat(response.rejectionReason()).isEqualTo("Período crítico");
    }

    @Test
    void cannotDecideTwice() {
        VacationRequest approved = request(34L, collaborator, date(8, 1), date(8, 5), VacationStatus.APROVADO);
        when(vacationRepo.findById(34L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> service.reject(auth(manager), 34L, null)).isInstanceOf(ConflictException.class);
    }

    // ------------------------------------------------------------ helpers

    private User user(Long id, String name, Role role, User manager) {
        User u = new User(name, name.toLowerCase().replace(' ', '.') + "@test.com", "hash", role, manager);
        ReflectionTestUtils.setField(u, "id", id);
        when(userRepo.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private VacationRequest request(Long id, User owner, LocalDate start, LocalDate end, VacationStatus status) {
        VacationRequest v = new VacationRequest(owner, start, end, null);
        ReflectionTestUtils.setField(v, "id", id);
        ReflectionTestUtils.setField(v, "status", status);
        return v;
    }

    private static AuthenticatedUser auth(User u) {
        return AuthenticatedUser.from(u);
    }

    private static LocalDate date(int month, int day) {
        return LocalDate.of(2026, month, day);
    }
}
