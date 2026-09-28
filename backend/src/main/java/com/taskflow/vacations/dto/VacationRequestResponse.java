package com.taskflow.vacations.dto;

import com.taskflow.vacations.model.VacationRequest;
import com.taskflow.vacations.model.VacationStatus;

import java.time.Instant;
import java.time.LocalDate;

/** Representação de um pedido de férias devolvida pela API. */
public record VacationRequestResponse(
        Long id,
        UserRef employee,
        LocalDate startDate,
        LocalDate endDate,
        long days,
        VacationStatus status,
        String notes,
        String rejectionReason,
        UserRef decidedBy,
        Instant decidedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static VacationRequestResponse from(VacationRequest v) {
        return new VacationRequestResponse(v.getId(), UserRef.from(v.getUser()), v.getStartDate(), v.getEndDate(),
                v.getDays(), v.getStatus(), v.getNotes(), v.getRejectionReason(), UserRef.from(v.getDecidedBy()),
                v.getDecidedAt(), v.getCreatedAt(), v.getUpdatedAt());
    }
}
