package com.taskflow.vacations.dto;

import com.taskflow.vacations.model.VacationStatus;

import java.time.LocalDate;

/**
 * Um período ocupado no calendário.
 * <p>
 * Como a regra de não sobreposição é GLOBAL, todos precisam de saber que dias já estão ocupados.
 * Mas o COLLABORATOR só pode ver os próprios pedidos (RF-007), por isso quando o utilizador não tem
 * permissão para ver o pedido, {@code id} e {@code employeeName} vêm a null e {@code visible = false}
 * — mostra-se apenas "Ocupado".
 */
public record CalendarEntry(Long id, LocalDate startDate, LocalDate endDate, VacationStatus status,
                            String employeeName, boolean visible) {
}
