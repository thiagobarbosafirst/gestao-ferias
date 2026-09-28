package com.taskflow.vacations.controller;

import com.taskflow.vacations.dto.*;
import com.taskflow.vacations.model.VacationStatus;
import com.taskflow.vacations.security.AuthenticatedUser;
import com.taskflow.vacations.service.VacationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Pedidos de férias (RF-004 a RF-009).
 * <p>
 * Todos os utilizadores autenticados acedem a estes endpoints; as permissões dependem de QUEM é o
 * dono do pedido, por isso são verificadas no {@link VacationRequestService}.
 */
@RestController
@RequestMapping("/api/vacations")
@Tag(name = "Pedidos de férias")
public class VacationRequestController {

    private final VacationRequestService service;

    public VacationRequestController(VacationRequestService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar pedidos visíveis para o utilizador (filtros + paginação)")
    public PageResponse<VacationRequestResponse> list(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) VacationStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        var filter = new VacationFilter(status, userId, employeeName, from, to);
        return service.list(actor, filter, pageable);
    }

    @GetMapping("/calendar")
    @Operation(summary = "Períodos ocupados (PENDENTE/APROVADO) entre from e to, para o calendário")
    public List<CalendarEntry> calendar(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.calendar(actor, from, to);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de um pedido")
    public VacationRequestResponse get(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return service.get(actor, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar pedido (fica PENDENTE). userId opcional: ADMIN para qualquer um, MANAGER para a equipa")
    public VacationRequestResponse create(@AuthenticationPrincipal AuthenticatedUser actor,
                                          @Valid @RequestBody VacationRequestCreate body) {
        return service.create(actor, body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar pedido PENDENTE")
    public VacationRequestResponse update(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                          @Valid @RequestBody VacationRequestUpdate body) {
        return service.update(actor, id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancelar (remover) pedido")
    public void cancel(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        service.cancel(actor, id);
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Aprovar pedido (manager responsável ou ADMIN)")
    public VacationRequestResponse approve(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id) {
        return service.approve(actor, id);
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Rejeitar pedido (manager responsável ou ADMIN), com motivo opcional")
    public VacationRequestResponse reject(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long id,
                                          @Valid @RequestBody(required = false) RejectRequest body) {
        return service.reject(actor, id, body == null ? null : body.reason());
    }
}
