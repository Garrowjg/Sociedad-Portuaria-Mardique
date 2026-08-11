package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByIdAsc(String sessionId);

    long deleteBySessionId(String sessionId);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);

    long countByRole(String role);

    long countByType(String type);

    long countByFeedback(String feedback);

    @Query("SELECT COUNT(DISTINCT m.sessionId) FROM ChatMessage m")
    long countDistinctSessions();

    @Query("SELECT m.sessionId, m.role, m.createdAt FROM ChatMessage m ORDER BY m.id ASC")
    List<Object[]> findAllSessionMessages();

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.role = 'assistant' AND LENGTH(m.content) >= 20 " +
            "AND m.content NOT LIKE 'Lo siento%' " +
            "AND m.content NOT LIKE 'No obtuve%' " +
            "AND m.content NOT LIKE '%ocurrió un error%' " +
            "AND m.content NOT LIKE '%no fue posible%'")
    long countAccurateResponses();
}
