package com.taskflow.vacations.service;

import com.taskflow.vacations.dto.*;
import com.taskflow.vacations.exception.BusinessException;
import com.taskflow.vacations.exception.ConflictException;
import com.taskflow.vacations.exception.ForbiddenException;
import com.taskflow.vacations.exception.NotFoundException;
import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.model.User;
import com.taskflow.vacations.model.VacationRequest;
import com.taskflow.vacations.model.VacationStatus;
import com.taskflow.vacations.repository.UserRepository;
import com.taskflow.vacations.repository.VacationRequestRepository;
import com.taskflow.vacations.repository.VacationRequestSpecifications;
import com.taskflow.vacations.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Regras de negócio dos pedidos de férias (RF-004 a RF-009, RB-001, RB-002).
 *
 * <h2>Permissões (resumo)</h2>
 * <pre>
 *                       ADMIN   MANAGER (próprios + equipa)   COLLABORATOR (só próprios)
 * ver / listar           todos   sim                           sim
 * criar / editar         todos   sim                           sim
 * cancelar               todos   sim                           sim
 * aprovar / rejeitar     todos   só EQUIPA (não os próprios)   não
 * </pre>
 *
 * <h2>Ciclo de vida</h2>
 * PENDENTE → APROVADO ou REJEITADO. Só pedidos PENDENTES podem ser editados.
 * "Cancelar" remove o pedido (o enunciado só define 3 estados, por isso não existe "CANCELADO").
 */
@Service
@Transactional
public class VacationRequestService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Limite do intervalo pedido ao calendário, para evitar queries enormes. */
    private static final long MAX_CALENDAR_DAYS = 366;

    private final VacationRequestRepository vacationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public VacationRequestService(VacationRequestRepository vacationRepository, UserRepository userRepository,
                                  Clock clock) {
        this.vacationRepository = vacationRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ consultas

    /** Lista os pedidos que o utilizador pode ver, com filtros e paginação. */
    @Transactional(readOnly = true)
    public PageResponse<VacationRequestResponse> list(AuthenticatedUser principal, VacationFilter filter,
                                                      Pageable pageable) {
        User actor = loadActor(principal);
        var spec = VacationRequestSpecifications.visibleWithFilters(actor, filter);
        return PageResponse.from(vacationRepository.findAll(spec, pageable), VacationRequestResponse::from);
    }

    /** Detalhe de um pedido (403 se o utilizador não o puder ver). */
    @Transactional(readOnly = true)
    public VacationRequestResponse get(AuthenticatedUser principal, Long id) {
        User actor = loadActor(principal);
        VacationRequest request = findRequest(id);
        if (!canManage(actor, request.getUser())) {
            throw new ForbiddenException("Não tem permissão para ver este pedido");
        }
        return VacationRequestResponse.from(request);
    }

    /**
     * Períodos ocupados entre {@code from} e {@code to}, para o calendário.
     * Todos veem QUE dias estão ocupados (a regra de não sobreposição é global),
     * mas só veem QUEM está de férias quando têm permissão para ver esse pedido.
     */
    @Transactional(readOnly = true)
    public List<CalendarEntry> calendar(AuthenticatedUser principal, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessException("A data 'to' não pode ser anterior a 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_CALENDAR_DAYS) {
            throw new BusinessException("O intervalo do calendário não pode exceder " + MAX_CALENDAR_DAYS + " dias");
        }
        User actor = loadActor(principal);
        var spec = VacationRequestSpecifications.occupyingBetween(from, to);
        return vacationRepository.findAll(spec, Sort.by("startDate")).stream()
                .map(v -> {
                    boolean visible = canManage(actor, v.getUser());
                    return new CalendarEntry(
                            visible ? v.getId() : null,
                            v.getStartDate(), v.getEndDate(), v.getStatus(),
                            visible ? v.getUser().getName() : null,
                            visible);
                })
                .toList();
    }

    // ------------------------------------------------------------------ operações

    /**
     * Cria um pedido (sempre PENDENTE).
     * Sem {@code userId} o pedido é para o próprio; ADMIN pode criar para qualquer pessoa
     * e MANAGER para alguém da sua equipa.
     */
    public VacationRequestResponse create(AuthenticatedUser principal, VacationRequestCreate body) {
        User actor = loadActor(principal);
        User owner = body.userId() == null || body.userId().equals(actor.getId())
                ? actor
                : userRepository.findById(body.userId())
                        .orElseThrow(() -> new NotFoundException("Colaborador " + body.userId() + " não encontrado"));

        if (!canManage(actor, owner)) {
            throw new ForbiddenException("Só pode criar pedidos de férias para si próprio"
                    + (actor.getRole() == Role.MANAGER ? " ou para a sua equipa" : ""));
        }
        validateDates(body.startDate(), body.endDate());
        ensureNoOverlap(actor, body.startDate(), body.endDate(), null);

        VacationRequest request = new VacationRequest(owner, body.startDate(), body.endDate(), trimToNull(body.notes()));
        // saveAndFlush: força o INSERT agora, para que uma violação da constraint na BD
        // (pedido concorrente) seja convertida em 409 pelo GlobalExceptionHandler.
        return VacationRequestResponse.from(vacationRepository.saveAndFlush(request));
    }

    /** Edita datas/observações de um pedido PENDENTE. */
    public VacationRequestResponse update(AuthenticatedUser principal, Long id, VacationRequestUpdate body) {
        User actor = loadActor(principal);
        VacationRequest request = findRequest(id);

        if (!canManage(actor, request.getUser())) {
            throw new ForbiddenException("Não tem permissão para editar este pedido");
        }
        if (request.getStatus() != VacationStatus.PENDENTE) {
            throw new ConflictException("Só é possível editar pedidos PENDENTES (estado atual: "
                    + request.getStatus() + ")");
        }
        validateDates(body.startDate(), body.endDate());
        // Exclui o próprio pedido da verificação, senão ele "sobrepunha-se a si mesmo".
        ensureNoOverlap(actor, body.startDate(), body.endDate(), request.getId());

        request.update(body.startDate(), body.endDate(), trimToNull(body.notes()));
        return VacationRequestResponse.from(vacationRepository.saveAndFlush(request));
    }

    /**
     * Cancela (remove) um pedido. Pedidos REJEITADOS ficam como histórico e não podem ser cancelados.
     * Férias APROVADAS que já começaram só podem ser canceladas por um ADMIN.
     */
    public void cancel(AuthenticatedUser principal, Long id) {
        User actor = loadActor(principal);
        VacationRequest request = findRequest(id);

        if (!canManage(actor, request.getUser())) {
            throw new ForbiddenException("Não tem permissão para cancelar este pedido");
        }
        if (request.getStatus() == VacationStatus.REJEITADO) {
            throw new ConflictException("Pedidos rejeitados não podem ser cancelados");
        }
        boolean alreadyStarted = !request.getStartDate().isAfter(today());
        if (request.getStatus() == VacationStatus.APROVADO && alreadyStarted && !actor.isAdmin()) {
            throw new ConflictException("Não é possível cancelar férias aprovadas que já começaram. Contacte um administrador.");
        }
        vacationRepository.delete(request);
    }

    /**
     * Aprova um pedido PENDENTE (RF-006).
     * Não é preciso voltar a verificar sobreposição: um pedido PENDENTE já "reserva" os dias,
     * logo nenhum outro pedido pode ter sido criado por cima dele.
     */
    public VacationRequestResponse approve(AuthenticatedUser principal, Long id) {
        User actor = loadActor(principal);
        VacationRequest request = findPendingForDecision(actor, id);
        request.approve(actor);
        return VacationRequestResponse.from(vacationRepository.saveAndFlush(request));
    }

    /** Rejeita um pedido PENDENTE (RF-006). Os dias ficam livres para outros colaboradores. */
    public VacationRequestResponse reject(AuthenticatedUser principal, Long id, String reason) {
        User actor = loadActor(principal);
        VacationRequest request = findPendingForDecision(actor, id);
        request.reject(actor, trimToNull(reason));
        return VacationRequestResponse.from(vacationRepository.saveAndFlush(request));
    }

    // ------------------------------------------------------------------ regras / permissões

    /**
     * Pode ver/criar/editar/cancelar pedidos de {@code owner}?
     * ADMIN: sempre. Qualquer um: os próprios. MANAGER: os da sua equipa.
     */
    static boolean canManage(User actor, User owner) {
        return actor.isAdmin()
                || actor.getId().equals(owner.getId())
                || owner.isManagedBy(actor);
    }

    /**
     * Pode aprovar/rejeitar pedidos de {@code owner}? (RF-006, RF-008)
     * Só ADMIN ou o manager DIRETO. Um manager não aprova os próprios pedidos.
     */
    static boolean canDecide(User actor, User owner) {
        return actor.isAdmin() || owner.isManagedBy(actor);
    }

    private VacationRequest findPendingForDecision(User actor, Long id) {
        VacationRequest request = findRequest(id);
        if (!canDecide(actor, request.getUser())) {
            throw new ForbiddenException("Só o manager responsável ou um ADMIN pode aprovar/rejeitar este pedido");
        }
        if (request.getStatus() != VacationStatus.PENDENTE) {
            throw new ConflictException("O pedido já foi decidido (estado atual: " + request.getStatus() + ")");
        }
        return request;
    }

    /** Validações de datas que o Bean Validation não consegue fazer sozinho. */
    private void validateDates(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessException("A data de fim não pode ser anterior à data de início");
        }
        if (start.isBefore(today())) {
            throw new BusinessException("Não é possível marcar férias em datas passadas");
        }
    }

    /**
     * RB-001: garante que nenhum outro pedido PENDENTE ou APROVADO ocupa algum dia de [start, end].
     * As datas são inclusivas (RB-002), por isso 01/08→05/08 e 05/08→10/08 entram em conflito.
     * A mensagem só revela o nome do outro colaborador se o utilizador tiver permissão para o ver.
     */
    private void ensureNoOverlap(User actor, LocalDate start, LocalDate end, Long excludeId) {
        List<VacationRequest> overlapping = vacationRepository.findOverlapping(
                start, end, VacationStatus.BLOCKING, excludeId == null ? -1L : excludeId);
        if (overlapping.isEmpty()) {
            return;
        }
        VacationRequest conflict = overlapping.getFirst();
        String period = fmt(conflict.getStartDate()) + " a " + fmt(conflict.getEndDate());
        String who = canManage(actor, conflict.getUser())
                ? "às férias de " + conflict.getUser().getName()
                : "a férias já marcadas por outro colaborador";
        throw new ConflictException("O período de " + fmt(start) + " a " + fmt(end) + " sobrepõe-se "
                + who + " (" + period + ", " + conflict.getStatus() + ")");
    }

    private User loadActor(AuthenticatedUser principal) {
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("Utilizador autenticado não encontrado"));
    }

    private VacationRequest findRequest(Long id) {
        return vacationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pedido de férias " + id + " não encontrado"));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static String fmt(LocalDate date) {
        return date.format(DATE_FMT);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
