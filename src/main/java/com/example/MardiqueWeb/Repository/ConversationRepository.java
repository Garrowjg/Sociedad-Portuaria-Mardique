package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findAllByOrderByCreatedAtDesc();
}
