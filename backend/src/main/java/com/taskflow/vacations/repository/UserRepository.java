package com.taskflow.vacations.repository;

import com.taskflow.vacations.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Acesso a dados de utilizadores. O Spring Data gera a implementação automaticamente
 * a partir do nome dos métodos.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Usado para impedir remover/despromover um manager que ainda tem equipa. */
    boolean existsByManagerId(Long managerId);
}
