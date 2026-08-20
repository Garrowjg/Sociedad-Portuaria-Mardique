package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intranet_documents", indexes = {
        @Index(name = "idx_intranet_docs_sector", columnList = "sector")
})
public class IntranetDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sector o carpeta de la intranet: "rrhh", "finanzas", "ti", "general", etc.
    @Column(nullable = false, length = 64)
    private String sector;

    // Nombre original del archivo subido
    @Column(nullable = false)
    private String nombre;

    // Nombre con el que se guarda en disco (UUID.ext) para evitar colisiones y path traversal
    @Column(name = "stored_name")
    private String storedName;

    // true si es una carpeta (no tiene archivo físico); false para archivos.
    // Se usa Boolean (no boolean) porque filas creadas antes de agregar la columna
    // pueden tener NULL en la base de datos y hay que tolerarlo al leerlas.
    @Column(name = "es_carpeta")
    private Boolean esCarpeta = false;

    // Si está dentro de una carpeta, id de la carpeta padre; null = raíz del sector
    @Column(name = "parent_id")
    private Long parentId;

    // Extensión sin punto, en minúsculas (pdf, docx, png, ...)
    @Column(name = "file_type", nullable = false, length = 32)
    private String fileType;

    @Column(name = "file_size")
    private long fileSize;

    @Column(name = "uploaded_by", length = 120)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public IntranetDocument() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSector() {
        return sector;
    }

    public void setSector(String sector) {
        this.sector = sector;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getStoredName() {
        return storedName;
    }

    public void setStoredName(String storedName) {
        this.storedName = storedName;
    }

    public boolean isEsCarpeta() {
        return esCarpeta != null && esCarpeta;
    }

    public void setEsCarpeta(Boolean esCarpeta) {
        this.esCarpeta = esCarpeta;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
