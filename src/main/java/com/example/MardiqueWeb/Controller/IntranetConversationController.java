package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.Comment;
import com.example.MardiqueWeb.Entity.Conversation;
import com.example.MardiqueWeb.Repository.CommentRepository;
import com.example.MardiqueWeb.Repository.ConversationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intranet/conversations")
public class IntranetConversationController {

    private static final Logger log = LoggerFactory.getLogger(IntranetConversationController.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ConversationRepository repo;
    private final CommentRepository commentRepo;

    public IntranetConversationController(ConversationRepository repo, CommentRepository commentRepo) {
        this.repo = repo;
        this.commentRepo = commentRepo;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) String userId) {
        List<Conversation> conversations = repo.findAllByOrderByPinnedDescCreatedAtDesc();
        String uid = (userId != null && !userId.isEmpty()) ? userId : "";
        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversation c : conversations) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("authorName", c.getAuthorName());
            map.put("authorEmail", c.getAuthorEmail());
            map.put("title", c.getTitle());
            map.put("text", c.getText());
            map.put("type", c.getType());
            map.put("photosJson", c.getPhotosJson());
            map.put("pinned", c.isPinned());
            map.put("likes", c.getLikes());
            map.put("likedBy", c.getLikedBy());
            map.put("reactionsJson", c.getReactionsJson());
            map.put("comments", c.getComments());
            map.put("createdAt", c.getCreatedAt());
            boolean userLiked = false;
            String userReaction = "";
            if (!uid.isEmpty()) {
                String likedBy = c.getLikedBy() != null ? c.getLikedBy() : "";
                List<String> users = new ArrayList<>(List.of(likedBy.split(",")));
                users.removeIf(String::isEmpty);
                userLiked = users.contains(uid);
                try {
                    Map<String, List<String>> reactions = mapper.readValue(
                        c.getReactionsJson() != null ? c.getReactionsJson() : "{}",
                        new TypeReference<Map<String, List<String>>>() {}
                    );
                    for (Map.Entry<String, List<String>> entry : reactions.entrySet()) {
                        if (entry.getValue().contains(uid)) {
                            userReaction = entry.getKey();
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
            map.put("userLiked", userLiked);
            map.put("userReaction", userReaction);
            result.add(map);
        }
        return result;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Conversation c = new Conversation();
        c.setAuthorName((String) body.getOrDefault("author", "Empleado"));
        c.setAuthorEmail((String) body.getOrDefault("authorEmail", ""));
        c.setTitle((String) body.getOrDefault("title", ""));
        c.setText((String) body.getOrDefault("text", ""));
        c.setType((String) body.getOrDefault("type", "Discusión"));
        c.setPhotosJson((String) body.getOrDefault("photosJson", ""));
        c.setPinned(false);
        c.setLikes(0);
        c.setComments(0);
        c.setLikedBy("");
        c.setReactionsJson("{}");
        repo.save(c);
        return ResponseEntity.ok(c);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Conversation c = repo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        if (body.containsKey("title")) c.setTitle((String) body.get("title"));
        if (body.containsKey("text")) c.setText((String) body.get("text"));
        if (body.containsKey("photosJson")) c.setPhotosJson((String) body.get("photosJson"));
        repo.save(c);
        return ResponseEntity.ok(c);
    }

    @PostMapping("/{id}/pin")
    public ResponseEntity<?> togglePin(@PathVariable Long id) {
        Conversation c = repo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        c.setPinned(!c.isPinned());
        repo.save(c);
        return ResponseEntity.ok(Map.of("pinned", c.isPinned()));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        Conversation c = repo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String userId = body != null ? body.getOrDefault("userId", "") : "";
        if (userId.isEmpty()) userId = "anonymous";
        String likedBy = c.getLikedBy() != null ? c.getLikedBy() : "";
        List<String> users = new ArrayList<>(List.of(likedBy.split(",")));
        users.removeIf(String::isEmpty);
        if (users.contains(userId)) {
            users.remove(userId);
            c.setLikes(Math.max(0, c.getLikes() - 1));
        } else {
            users.add(userId);
            c.setLikes(c.getLikes() + 1);
        }
        c.setLikedBy(String.join(",", users));
        repo.save(c);
        return ResponseEntity.ok(Map.of("likes", c.getLikes(), "userLiked", users.contains(userId)));
    }

    @PostMapping("/{id}/reaction")
    public ResponseEntity<?> toggleReaction(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Conversation c = repo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String userId = body.getOrDefault("userId", "");
        String reactionType = body.getOrDefault("type", "like");
        if (userId.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "userId required"));

        try {
            Map<String, List<String>> reactions = mapper.readValue(
                c.getReactionsJson() != null ? c.getReactionsJson() : "{}",
                new TypeReference<Map<String, List<String>>>() {}
            );

            String existingType = "";
            for (Map.Entry<String, List<String>> entry : reactions.entrySet()) {
                if (entry.getValue().contains(userId)) {
                    existingType = entry.getKey();
                    break;
                }
            }

            if (existingType.equals(reactionType)) {
                List<String> list = reactions.getOrDefault(reactionType, new ArrayList<>());
                list.remove(userId);
                if (list.isEmpty()) reactions.remove(reactionType);
                else reactions.put(reactionType, list);
            } else {
                if (!existingType.isEmpty()) {
                    List<String> prevList = reactions.getOrDefault(existingType, new ArrayList<>());
                    prevList.remove(userId);
                    if (prevList.isEmpty()) reactions.remove(existingType);
                    else reactions.put(existingType, prevList);
                }
                List<String> list = reactions.computeIfAbsent(reactionType, k -> new ArrayList<>());
                list.add(userId);
            }

            int totalCount = 0;
            for (List<String> v : reactions.values()) totalCount += v.size();
            c.setLikes(totalCount);

            c.setLikedBy("");
            List<String> allUsers = new ArrayList<>();
            for (List<String> v : reactions.values()) allUsers.addAll(v);
            c.setLikedBy(String.join(",", allUsers));

            c.setReactionsJson(mapper.writeValueAsString(reactions));
            repo.save(c);

            String userReaction = "";
            for (Map.Entry<String, List<String>> entry : reactions.entrySet()) {
                if (entry.getValue().contains(userId)) {
                    userReaction = entry.getKey();
                    break;
                }
            }
            return ResponseEntity.ok(Map.of(
                "likes", c.getLikes(),
                "reactionsJson", c.getReactionsJson(),
                "userReaction", userReaction
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.ok(Map.of("comments", List.of(), "count", 0));
        }
        List<Comment> comments = commentRepo.findByConversationIdOrderByCreatedAtAsc(id);
        long count = commentRepo.countByConversationId(id);
        return ResponseEntity.ok(Map.of("comments", comments, "count", count));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Conversation c = repo.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        String text = body.getOrDefault("text", "").trim();
        if (text.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "El comentario no puede estar vacío"));
        Comment comment = new Comment();
        comment.setConversationId(id);
        comment.setAuthorName(body.getOrDefault("authorName", "Empleado"));
        comment.setAuthorEmail(body.getOrDefault("authorEmail", ""));
        comment.setText(text);
        commentRepo.save(comment);
        c.setComments((int) commentRepo.countByConversationId(id));
        repo.save(c);
        return ResponseEntity.ok(Map.of("comment", comment, "count", c.getComments()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        commentRepo.deleteByConversationId(id);
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
