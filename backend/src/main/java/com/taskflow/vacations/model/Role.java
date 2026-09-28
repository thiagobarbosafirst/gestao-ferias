package com.taskflow.vacations.model;

/**
 * Papéis (roles) existentes no sistema.
 * <ul>
 *   <li>ADMIN: gere utilizadores e todos os pedidos de férias.</li>
 *   <li>MANAGER: aprova/rejeita os pedidos da sua equipa.</li>
 *   <li>COLLABORATOR: gere apenas os próprios pedidos.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    MANAGER,
    COLLABORATOR
}
