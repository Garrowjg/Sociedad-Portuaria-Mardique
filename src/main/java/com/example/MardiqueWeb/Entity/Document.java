package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String tipo;

    @Column(name = "card_key")
    private String cardKey;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "uploaded_at")
    private LocalDate uploadedAt = LocalDate.now();

    private String email;

    @Column(name = "email_cc")
    private String emailCc;

    private String descripcion;

    public Document() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getCardKey() { return cardKey; }
    public void setCardKey(String cardKey) { this.cardKey = cardKey; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public LocalDate getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDate uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailCc() { return emailCc; }
    public void setEmailCc(String emailCc) { this.emailCc = emailCc; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
