package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Un turno individual de una conversación del chatbot (pregunta del usuario o
 * respuesta del asistente), persistido en Postgres para poder reconstruir el
 * historial real de cada sesión y pasárselo de vuelta al modelo.
 */
@Entity
@Table(name = "chatbot_messages", indexes = {
        @Index(name = "idx_chatbot_messages_session", columnList = "sessionId")
})
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sessionId;

    // "user" o "assistant"
    @Column(nullable = false, length = 16)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Clasificación del turno del asistente: "faq" | "form" | "out_of_bounds" | "llm_rag" | "llm" (null en mensajes de usuario)
    @Column(length = 32)
    private String type;

    // Voto del usuario: "up" (útil) | "down" (no útil) | null (sin votar)
    @Column(length = 8)
    private String feedback;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessage() {
    }

    public ChatMessage(String sessionId, String role, String content) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(String sessionId, String role, String content, String type) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
