package com.taskflow.vacations.service;

import com.taskflow.vacations.dto.PageResponse;
import com.taskflow.vacations.dto.UserCreateRequest;
import com.taskflow.vacations.dto.UserResponse;
import com.taskflow.vacations.dto.UserUpdateRequest;
import com.taskflow.vacations.exception.BusinessException;
import com.taskflow.vacations.exception.ConflictException;
import com.taskflow.vacations.exception.NotFoundException;
import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.model.User;
import com.taskflow.vacations.repository.UserRepository;
import com.taskflow.vacations.repository.UserSpecifications;
import com.taskflow.vacations.repository.VacationRequestRepository;
import com.taskflow.vacations.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Regras de negócio da gestão de colaboradores (RF-001, RF-002, RF-003).
 * <p>
 * A restrição "só ADMIN" é aplicada no controller com {@code @PreAuthorize}; aqui ficam as regras
 * sobre os DADOS (email único, manager válido, não remover managers com equipa, ...).
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final VacationRequestRepository vacationRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, VacationRequestRepository vacationRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.vacationRepository = vacationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lista utilizadores com filtros opcionais e paginação. */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String search, Role role, Long managerId, Pageable pageable) {
        return PageResponse.from(
                userRepository.findAll(UserSpecifications.withFilters(search, role, managerId), pageable),
                UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(findUser(id));
    }

    /** Cria um utilizador. A password é guardada como hash BCrypt. */
    public UserResponse create(UserCreateRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Já existe um utilizador com o email " + email);
        }
        User manager = resolveManager(request.role(), request.managerId(), null);

        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()),
                request.role(), manager);
        return UserResponse.from(userRepository.save(user));
    }

    /** Edita um utilizador. Se a password vier vazia mantém-se a atual. */
    public UserResponse update(Long id, UserUpdateRequest request, AuthenticatedUser actor) {
        User user = findUser(id);
        String email = normalizeEmail(request.email());

        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Já existe um utilizador com o email " + email);
        }
        // Evita que o admin perca o próprio acesso de administração por engano.
        if (user.getId().equals(actor.id()) && request.role() != user.getRole()) {
            throw new BusinessException("Não pode alterar o seu próprio role");
        }
        // Um manager com equipa não pode deixar de ser manager (a equipa ficaria sem manager).
        if (user.getRole() == Role.MANAGER && request.role() != Role.MANAGER
                && userRepository.existsByManagerId(user.getId())) {
            throw new ConflictException("Este manager tem colaboradores associados. Reatribua-os antes de mudar o role.");
        }

        user.setName(request.name().trim());
        user.setEmail(email);
        user.setRole(request.role());
        user.setManager(resolveManager(request.role(), request.managerId(), user.getId()));
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(user); // alterações gravadas automaticamente no fim da transação
    }

    /**
     * Remove um utilizador e os respetivos pedidos de férias.
     * Não permite remover o próprio utilizador nem um manager que ainda tenha equipa.
     */
    public void delete(Long id, AuthenticatedUser actor) {
        User user = findUser(id);
        if (user.getId().equals(actor.id())) {
            throw new BusinessException("Não pode remover o seu próprio utilizador");
        }
        if (userRepository.existsByManagerId(user.getId())) {
            throw new ConflictException("Este manager tem colaboradores associados. Reatribua-os antes de o remover.");
        }
        vacationRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    /**
     * Valida e devolve o manager a associar (RF-002):
     * <ul>
     *   <li>COLLABORATOR: manager obrigatório;</li>
     *   <li>MANAGER: manager opcional (ex.: um diretor);</li>
     *   <li>ADMIN: não tem manager (qualquer valor é ignorado);</li>
     *   <li>o manager indicado tem de existir, ter role MANAGER e não ser o próprio utilizador.</li>
     * </ul>
     */
    private User resolveManager(Role role, Long managerId, Long selfId) {
        if (role == Role.ADMIN) {
            return null;
        }
        if (managerId == null) {
            if (role == Role.COLLABORATOR) {
                throw new BusinessException("Um colaborador tem de estar associado a um manager");
            }
            return null;
        }
        if (Objects.equals(managerId, selfId)) {
            throw new BusinessException("Um utilizador não pode ser o seu próprio manager");
        }
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new BusinessException("O manager indicado não existe"));
        if (manager.getRole() != Role.MANAGER) {
            throw new BusinessException("O utilizador indicado como manager não tem o role MANAGER");
        }
        return manager;
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilizador " + id + " não encontrado"));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
