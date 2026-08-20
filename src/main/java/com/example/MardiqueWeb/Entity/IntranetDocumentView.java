package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intranet_document_views", indexes = {
        @Index(name = "idx_intranet_doc_views_doc", columnList = "document_id"),
        @Index(name = "idx_intranet_doc_views_email", columnList = "viewer_email")
})
public class IntranetDocumentView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Documento visto (id de IntranetDocument)
    @Column(name = "document_id", nullable = false)
    private Long documentId;

    // Correo de quien lo vio (cuenta de Microsoft)
    @Column(name = "viewer_email", nullable = false, length = 180)
    private String viewerEmail;

    @Column(name = "viewer_name", length = 120)
    private String viewerName;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt = LocalDateTime.now();

    public IntranetDocumentView() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getViewerEmail() {
        return viewerEmail;
    }

    public void setViewerEmail(String viewerEmail) {
        this.viewerEmail = viewerEmail;
    }

    public String getViewerName() {
        return viewerName;
    }

    public void setViewerName(String viewerName) {
        this.viewerName = viewerName;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}