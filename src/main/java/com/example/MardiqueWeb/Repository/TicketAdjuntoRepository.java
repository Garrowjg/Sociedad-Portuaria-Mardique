package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.TicketAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketAdjuntoRepository extends JpaRepository<TicketAdjunto, Long> {
    List<TicketAdjunto> findByTicketIdOrderByFechaAsc(Long ticketId);
}