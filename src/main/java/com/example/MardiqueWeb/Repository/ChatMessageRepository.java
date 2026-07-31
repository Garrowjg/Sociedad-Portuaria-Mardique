package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByIdAsc(String sessionId);

    long deleteBySessionId(String sessionId);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
