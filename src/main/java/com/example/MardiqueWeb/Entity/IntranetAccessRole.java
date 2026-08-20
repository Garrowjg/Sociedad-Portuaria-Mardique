package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intranet_access_roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_intranet_access_email", columnNames = "email")
})
public class IntranetAccessRole {

    // Roles de la intranet:
    //  - "ADMIN"   → coordinador de sistemas, control total (panel oculto)
    //  - "VIEWER"  → puede ver el botón "Ver quiénes han visto el documento"
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_VIEWER = "VIEWER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public IntranetAccessRole() {
    }

    public IntranetAccessRole(String email, String role) {
        this.email = email;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}