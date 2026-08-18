package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.TicketHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketHistorialRepository extends JpaRepository<TicketHistorial, Long> {
    List<TicketHistorial> findByTicketIdOrderByFechaAsc(Long ticketId);
    List<TicketHistorial> findByTicketIdOrderByFechaDesc(Long ticketId);
}