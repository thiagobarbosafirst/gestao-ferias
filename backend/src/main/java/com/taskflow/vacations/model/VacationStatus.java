package com.taskflow.vacations.model;

/**
 * Estados possíveis de um pedido de férias (RF-005).
 */
public enum VacationStatus {
    /** Criado, à espera de decisão. Já "reserva" os dias (conta para a regra de não sobreposição). */
    PENDENTE,
    /** Aprovado pelo manager responsável ou por um ADMIN. */
    APROVADO,
    /** Rejeitado. Deixa de ocupar os dias, que ficam livres para outros colaboradores. */
    REJEITADO;

    /** Estados que ocupam dias no calendário e por isso contam para a regra RB-001. */
    public static final java.util.List<VacationStatus> BLOCKING = java.util.List.of(PENDENTE, APROVADO);
}
