package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "faqs")
public class Faq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String question;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;

    private boolean activo = true;

    private int orden = 0;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Faq() {}

    public Faq(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
