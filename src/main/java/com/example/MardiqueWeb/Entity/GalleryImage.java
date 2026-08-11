package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "gallery_images")
public class GalleryImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String categoria;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "uploaded_at")
    private LocalDate uploadedAt = LocalDate.now();

    private boolean active = true;

    public GalleryImage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getCategoriaLabel() {
        return switch (categoria == null ? "" : categoria) {
            case "aerea" -> "Vista Aérea";
            case "operaciones" -> "Operaciones";
            case "instalaciones" -> "Instalaciones";
            case "equipos" -> "Equipos";
            default -> "Galería";
        };
    }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public LocalDate getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDate uploadedAt) { this.uploadedAt = uploadedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
