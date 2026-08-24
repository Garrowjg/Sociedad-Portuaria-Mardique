package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.Conversation;
import com.example.MardiqueWeb.Repository.ConversationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/intranet/conversations")
public class IntranetConversationController {

    private final ConversationRepository repo;

    public IntranetConversationController(ConversationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public java.util.List<Conversation> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Conversation c = new Conversation();
        c.setAuthorName((String) body.getOrDefault("author", "Empleado"));
        c.setAuthorEmail((String) body.getOrDefault("authorEmail", ""));
        c.setText((String) body.getOrDefault("text", ""));
        c.setType((String) body.getOrDefault("type", "Discusión"));
        c.setLikes(0);
        c.setComments(0);
        repo.save(c);
        return ResponseEntity.ok(c);
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> like(@PathVariable Long id) {
        Conversation c = repo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        c.setLikes(c.getLikes() + 1);
        repo.save(c);
        return ResponseEntity.ok(Map.of("likes", c.getLikes()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
