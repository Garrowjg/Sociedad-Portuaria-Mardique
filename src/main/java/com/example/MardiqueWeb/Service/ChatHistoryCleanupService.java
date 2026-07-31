package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Elimina automáticamente los mensajes del chatbot más antiguos que el número de
 * días configurado, para que la tabla chatbot_messages no crezca sin control con
 * sesiones huérfanas de visitantes anónimos.
 */
@Component
public class ChatHistoryCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryCleanupService.class);

    // Edad máxima de los mensajes antes de ser purgados
    private static final int RETENTION_DAYS = 7;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // Corre todos los días a las 3:00 AM
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldMessages() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
            long deleted = chatMessageRepository.deleteByCreatedAtBefore(cutoff);
            if (deleted > 0) {
                log.info("Chatbot cleanup: {} mensajes antiguos eliminados (más de {} días).", deleted, RETENTION_DAYS);
            }
        } catch (Exception e) {
            log.error("Chatbot cleanup error: {}", e.getMessage(), e);
        }
    }
}
