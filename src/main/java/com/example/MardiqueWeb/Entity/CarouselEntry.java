package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "carousel_entries")
public class CarouselEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String section;

    @Column(name = "image_key")
    private String imageKey;

    @Column(name = "file_path")
    private String filePath;

    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    public CarouselEntry() {}

    public CarouselEntry(String section, String imageKey, String filePath, String titulo, String descripcion) {
        this.section = section;
        this.imageKey = imageKey;
        this.filePath = filePath;
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}