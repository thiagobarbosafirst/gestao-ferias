package com.taskflow.vacations.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pedido de férias de um utilizador.
 * As datas {@link #startDate} e {@link #endDate} são INCLUSIVAS (RB-002).
 */
@Entity
@Table(name = "vacation_requests")
public class VacationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dono do pedido (quem vai de férias). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VacationStatus status;

    /** Observações opcionais do colaborador. */
    @Column(length = 500)
    private String notes;

    /** Motivo opcional quando o pedido é rejeitado. */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /** Quem aprovou/rejeitou o pedido. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VacationRequest() {
        // exigido pelo JPA
    }

    /** Cria um novo pedido, sempre no estado PENDENTE. */
    public VacationRequest(User user, LocalDate startDate, LocalDate endDate, String notes) {
        this.user = user;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
        this.status = VacationStatus.PENDENTE;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Número de dias do pedido, contando o primeiro e o último dia (RB-002).
     * Ex.: 01/08 → 05/08 = 5 dias.
     */
    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /** Altera as datas/observações (só permitido enquanto PENDENTE, validado no serviço). */
    public void update(LocalDate startDate, LocalDate endDate, String notes) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
    }

    public void approve(User decidedBy) {
        this.status = VacationStatus.APROVADO;
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
        this.rejectionReason = null;
    }

    public void reject(User decidedBy, String reason) {
        this.status = VacationStatus.REJEITADO;
        this.decidedBy = decidedBy;
        this.decidedAt = Instant.now();
        this.rejectionReason = reason;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public VacationStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getRejectionReason() { return rejectionReason; }
    public User getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
