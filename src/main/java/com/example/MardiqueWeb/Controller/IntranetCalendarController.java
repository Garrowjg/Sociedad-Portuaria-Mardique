package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.CalendarEvent;
import com.example.MardiqueWeb.Repository.CalendarEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/intranet/calendar")
public class IntranetCalendarController {

    private final CalendarEventRepository repo;

    public IntranetCalendarController(CalendarEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public java.util.List<CalendarEvent> list() {
        return repo.findAllByOrderByDateAsc();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        CalendarEvent ev = new CalendarEvent();
        ev.setTitle((String) body.getOrDefault("title", ""));
        ev.setDescription((String) body.getOrDefault("description", ""));
        ev.setType((String) body.getOrDefault("type", "general"));
        ev.setAuthorName((String) body.getOrDefault("authorName", "Empleado"));
        String dateStr = (String) body.getOrDefault("date", "");
        if (!dateStr.isEmpty()) {
            ev.setDate(LocalDate.parse(dateStr));
        }
        repo.save(ev);
        return ResponseEntity.ok(ev);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        CalendarEvent ev = repo.findById(id).orElse(null);
        if (ev == null) return ResponseEntity.notFound().build();
        if (body.containsKey("title")) ev.setTitle((String) body.get("title"));
        if (body.containsKey("description")) ev.setDescription((String) body.get("description"));
        if (body.containsKey("type")) ev.setType((String) body.get("type"));
        if (body.containsKey("date")) {
            String dateStr = (String) body.get("date");
            if (dateStr != null && !dateStr.isEmpty()) ev.setDate(LocalDate.parse(dateStr));
        }
        repo.save(ev);
        return ResponseEntity.ok(ev);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
