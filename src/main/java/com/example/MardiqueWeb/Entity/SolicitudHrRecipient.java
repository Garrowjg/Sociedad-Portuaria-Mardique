package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "solicitudes_hr_recipients", indexes = {
    @Index(name = "idx_solhr_r_email", columnList = "recipient_email"),
    @Index(name = "idx_solhr_r_solicitud", columnList = "solicitud_id"),
    @Index(name = "idx_solhr_r_status", columnList = "status")
})
public class SolicitudHrRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "solicitud_id", nullable = false)
    private Long solicitudId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDIENTE";

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "response", columnDefinition = "TEXT")
    private String response;

    @Column(name = "signature_url", columnDefinition = "TEXT")
    private String signatureUrl;

    @Column(name = "signed_document_url", columnDefinition = "TEXT")
    private String signedDocumentUrl;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SolicitudHrRecipient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSolicitudId() { return solicitudId; }
    public void setSolicitudId(Long solicitudId) { this.solicitudId = solicitudId; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getOrderIndex() { return orderIndex != null ? orderIndex : 0; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public String getSignatureUrl() { return signatureUrl; }
    public void setSignatureUrl(String signatureUrl) { this.signatureUrl = signatureUrl; }

    public String getSignedDocumentUrl() { return signedDocumentUrl; }
    public void setSignedDocumentUrl(String signedDocumentUrl) { this.signedDocumentUrl = signedDocumentUrl; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
