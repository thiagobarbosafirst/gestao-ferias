package com.taskflow.vacations.controller;

import com.taskflow.vacations.dto.PageResponse;
import com.taskflow.vacations.dto.UserCreateRequest;
import com.taskflow.vacations.dto.UserResponse;
import com.taskflow.vacations.dto.UserUpdateRequest;
import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.security.AuthenticatedUser;
import com.taskflow.vacations.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Gestão de colaboradores/utilizadores (RF-001).
 * <p>
 * {@code @PreAuthorize("hasRole('ADMIN')")} na classe: TODOS os endpoints exigem ADMIN (RF-003).
 * Um MANAGER ou COLLABORATOR recebe 403.
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Colaboradores", description = "Apenas ADMIN")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Listar colaboradores (filtros: search = nome/email, role, managerId; paginação: page, size, sort)")
    public PageResponse<UserResponse> list(@RequestParam(required = false) String search,
                                           @RequestParam(required = false) Role role,
                                           @RequestParam(required = false) Long managerId,
                                           @ParameterObject @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC)
                                           Pageable pageable) {
        return userService.list(search, role, managerId, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de um colaborador")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar colaborador (COLLABORATOR exige managerId)")
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar colaborador (password opcional)")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request,
                               @AuthenticationPrincipal AuthenticatedUser actor) {
        return userService.update(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover colaborador (e os seus pedidos de férias)")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser actor) {
        userService.delete(id, actor);
    }
}
