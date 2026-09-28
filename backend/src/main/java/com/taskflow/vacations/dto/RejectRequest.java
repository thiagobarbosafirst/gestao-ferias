package com.taskflow.vacations.dto;

import jakarta.validation.constraints.Size;

/** Corpo opcional para rejeitar um pedido. */
public record RejectRequest(@Size(max = 500, message = "O motivo tem no máximo 500 caracteres") String reason) {
}
