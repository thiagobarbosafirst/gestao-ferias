package com.taskflow.vacations.repository;

import com.taskflow.vacations.dto.VacationFilter;
import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.model.User;
import com.taskflow.vacations.model.VacationRequest;
import com.taskflow.vacations.model.VacationStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Constrói as queries dinâmicas (JPA Criteria) da listagem de pedidos de férias.
 * Cada filtro só é aplicado se vier preenchido.
 */
public final class VacationRequestSpecifications {

    private VacationRequestSpecifications() {
    }

    /**
     * Combina a regra de VISIBILIDADE (quem pode ver o quê) com os filtros escolhidos pelo utilizador.
     * <ul>
     *   <li>ADMIN: vê todos os pedidos (RF-009).</li>
     *   <li>MANAGER: vê os próprios e os da sua equipa (RF-008).</li>
     *   <li>COLLABORATOR: vê só os próprios (RF-007).</li>
     * </ul>
     */
    public static Specification<VacationRequest> visibleWithFilters(User actor, VacationFilter filter) {
        return (root, query, cb) -> {
            Join<VacationRequest, User> owner = root.join("user");
            List<Predicate> predicates = new ArrayList<>();

            // 1) Visibilidade por role
            if (actor.getRole() == Role.MANAGER) {
                Join<User, User> ownerManager = owner.join("manager", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.equal(owner.get("id"), actor.getId()),
                        cb.equal(ownerManager.get("id"), actor.getId())));
            } else if (actor.getRole() == Role.COLLABORATOR) {
                predicates.add(cb.equal(owner.get("id"), actor.getId()));
            }

            // 2) Filtros opcionais
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.userId() != null) {
                predicates.add(cb.equal(owner.get("id"), filter.userId()));
            }
            if (filter.employeeName() != null && !filter.employeeName().isBlank()) {
                predicates.add(cb.like(cb.lower(owner.get("name")),
                        "%" + filter.employeeName().trim().toLowerCase() + "%"));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), filter.to()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Pedidos que ocupam dias (PENDENTE/APROVADO) e tocam no intervalo [from, to]. Usado no calendário. */
    public static Specification<VacationRequest> occupyingBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.and(
                root.get("status").in(VacationStatus.BLOCKING),
                cb.lessThanOrEqualTo(root.get("startDate"), to),
                cb.greaterThanOrEqualTo(root.get("endDate"), from));
    }
}
