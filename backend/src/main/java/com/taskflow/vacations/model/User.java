package com.taskflow.vacations.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Utilizador do sistema. Representa tanto colaboradores como managers e admins
 * (o papel está no campo {@link #role}).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Hash BCrypt da password (nunca guardamos a password em texto). */
    @Column(nullable = false, length = 100)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Manager responsável por este utilizador (RF-002). Pode ser nulo para ADMIN/MANAGER. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
        // exigido pelo JPA
    }

    public User(String name, String email, String password, Role role, User manager) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.manager = manager;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    /** Indica se {@code other} é o manager direto deste utilizador. */
    public boolean isManagedBy(User other) {
        return manager != null && other != null && manager.getId().equals(other.getId());
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public User getManager() { return manager; }
    public void setManager(User manager) { this.manager = manager; }
    public Instant getCreatedAt() { return createdAt; }
}
