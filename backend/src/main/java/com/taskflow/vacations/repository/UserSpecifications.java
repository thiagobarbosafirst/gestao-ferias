package com.taskflow.vacations.repository;

import com.taskflow.vacations.model.Role;
import com.taskflow.vacations.model.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de utilizadores.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    /**
     * @param search    procura no nome OU email (sem distinguir maiúsculas)
     * @param role      apenas utilizadores com este role
     * @param managerId apenas utilizadores com este manager
     */
    public static Specification<User> withFilters(String search, Role role, Long managerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (managerId != null) {
                predicates.add(cb.equal(root.get("manager").get("id"), managerId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
