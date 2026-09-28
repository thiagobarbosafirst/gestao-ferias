package com.taskflow.vacations.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Dados para criar um pedido de férias.
 *
 * @param userId opcional: se vier vazio o pedido é para o próprio utilizador autenticado.
 *               ADMIN pode indicar qualquer utilizador; MANAGER apenas alguém da sua equipa.
 */
public record VacationRequestCreate(
        @NotNull(message = "A data de início é obrigatória") LocalDate startDate,
        @NotNull(message = "A data de fim é obrigatória") LocalDate endDate,
        @Size(max = 500, message = "As observações têm no máximo 500 caracteres") String notes,
        Long userId) {
}
