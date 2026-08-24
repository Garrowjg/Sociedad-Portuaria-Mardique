package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intranet_areas")
public class IntranetArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(columnDefinition = "TEXT")
    private String contactos;

    @Column(columnDefinition = "TEXT")
    private String informe;

    private String cover;

    private String sitio;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public IntranetArea() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getContactos() { return contactos; }
    public void setContactos(String contactos) { this.contactos = contactos; }

    public String getInforme() { return informe; }
    public void setInforme(String informe) { this.informe = informe; }

    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }

    public String getSitio() { return sitio; }
    public void setSitio(String sitio) { this.sitio = sitio; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
