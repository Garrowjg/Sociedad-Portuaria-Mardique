package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    long countByConversationId(Long conversationId);
    void deleteByConversationId(Long conversationId);
}
