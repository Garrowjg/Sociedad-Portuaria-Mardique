package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByStatus(String status);
    List<SupportTicket> findByUsername(String username);
    long countByStatus(String status);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    List<SupportTicket> findByOrigenOrderByCreatedAtDesc(String origen);
    long countByOrigen(String origen);
}
