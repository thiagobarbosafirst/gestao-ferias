package com.taskflow.vacations.dto;

import com.taskflow.vacations.model.VacationStatus;

import java.time.LocalDate;

/**
 * Filtros opcionais da listagem de pedidos (todos podem ser null).
 *
 * @param status       apenas pedidos neste estado
 * @param userId       apenas pedidos deste colaborador
 * @param employeeName parte do nome do colaborador (sem distinguir maiúsculas)
 * @param from         pedidos que terminam a partir desta data
 * @param to           pedidos que começam até esta data
 */
public record VacationFilter(VacationStatus status, Long userId, String employeeName, LocalDate from, LocalDate to) {
}
