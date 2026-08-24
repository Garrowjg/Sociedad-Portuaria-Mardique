package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.IntranetGalleryEvent;
import com.example.MardiqueWeb.Repository.IntranetGalleryEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intranet/gallery")
public class IntranetGalleryController {

    private final IntranetGalleryEventRepository repo;

    public IntranetGalleryController(IntranetGalleryEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<IntranetGalleryEvent> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        IntranetGalleryEvent ev = new IntranetGalleryEvent();
        ev.setTitle((String) body.getOrDefault("title", ""));
        ev.setDescription((String) body.getOrDefault("description", ""));
        ev.setAuthorName((String) body.getOrDefault("authorName", "Empleado"));
        ev.setAuthorEmail((String) body.getOrDefault("authorEmail", ""));
        ev.setPhotosJson((String) body.getOrDefault("photosJson", "[]"));
        ev.setLikes(0);
        repo.save(ev);
        return ResponseEntity.ok(ev);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> like(@PathVariable Long id) {
        IntranetGalleryEvent ev = repo.findById(id).orElse(null);
        if (ev == null) return ResponseEntity.notFound().build();
        ev.setLikes(ev.getLikes() + 1);
        repo.save(ev);
        return ResponseEntity.ok(Map.of("likes", ev.getLikes()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
