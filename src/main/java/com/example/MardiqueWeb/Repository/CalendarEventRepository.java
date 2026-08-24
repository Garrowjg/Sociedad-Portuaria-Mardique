package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findAllByOrderByDateAsc();
    List<CalendarEvent> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
}
