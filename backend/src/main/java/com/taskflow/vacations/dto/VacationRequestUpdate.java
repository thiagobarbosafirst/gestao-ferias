package com.taskflow.vacations.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Dados para editar um pedido de férias (apenas enquanto PENDENTE). */
public record VacationRequestUpdate(
        @NotNull(message = "A data de início é obrigatória") LocalDate startDate,
        @NotNull(message = "A data de fim é obrigatória") LocalDate endDate,
        @Size(max = 500, message = "As observações têm no máximo 500 caracteres") String notes) {
}
