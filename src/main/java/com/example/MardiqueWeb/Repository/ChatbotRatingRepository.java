package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.ChatbotRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotRatingRepository extends JpaRepository<ChatbotRating, Long> {
}
