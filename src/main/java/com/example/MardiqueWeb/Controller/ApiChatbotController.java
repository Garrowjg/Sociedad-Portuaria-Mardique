package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.ChatMessage;
import com.example.MardiqueWeb.Entity.ChatbotRating;
import com.example.MardiqueWeb.Entity.Faq;
import com.example.MardiqueWeb.Entity.SupportTicket;
import com.example.MardiqueWeb.Repository.ChatMessageRepository;
import com.example.MardiqueWeb.Repository.ChatbotRatingRepository;
import com.example.MardiqueWeb.Repository.FaqRepository;
import com.example.MardiqueWeb.Repository.SupportTicketRepository;
import com.example.MardiqueWeb.Service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chatbot")
public class ApiChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private FaqRepository faqRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private ChatbotRatingRepository chatbotRatingRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // Máximo de mensajes previos (usuario+asistente) que se recuperan de Postgres por sesión
    private static final int MAX_HISTORY_MESSAGES = 20;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^[0-9+\\-\\s()]{7,20}$";
    private static final String CEDULA_REGEX = "^[0-9]{4,12}$";

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, Object> body) {
        String question = asString(body.get("question"));
        if (question == null || question.isBlank()) {
            return Map.of("answer", "Escribe una pregunta para poder ayudarte.", "form", false, "blocked", false);
        }
        if (question.length() > 500) {
            return Map.of("answer", "Tu pregunta es muy larga. Intenta resumirla en máximo 500 caracteres.", "form", false, "blocked", false);
        }
        int repeatCount = parseRepeatCount(asString(body.get("repeatCount")));
        String sessionId = resolveSessionId(asString(body.get("sessionId")));

        List<Map<String, String>> history = loadHistory(sessionId);
        Map<String, Object> result = chatbotService.ask(question, repeatCount, history);

        // Persistimos el turno completo en Postgres para que la próxima pregunta de esta
        // misma sesión recuerde de qué se viene hablando.
        chatMessageRepository.save(new ChatMessage(sessionId, "user", question));
        Object answerObj = result.get("answer");
        ChatMessage savedBot = chatMessageRepository.save(new ChatMessage(sessionId, "assistant", answerObj != null ? answerObj.toString() : "",
                String.valueOf(result.getOrDefault("type", "llm"))));

        Map<String, Object> response = new LinkedHashMap<>(result);
        response.put("sessionId", sessionId);
        response.put("messageId", savedBot.getId());
        return response;
    }

    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> askStream(@RequestBody Map<String, Object> body) {
        SseEmitter emitter = new SseEmitter(60000L);
        String question = asString(body.get("question"));
        int repeatCount = parseRepeatCount(asString(body.get("repeatCount")));
        if (question == null || question.isBlank()) {
            question = "hola";
        }
        if (question.length() > 500) {
            question = question.substring(0, 500);
        }
        final String q = question;
        final String sessionId = resolveSessionId(asString(body.get("sessionId")));
        List<Map<String, String>> history = loadHistory(sessionId);

        chatMessageRepository.save(new ChatMessage(sessionId, "user", q));

        CompletableFuture.runAsync(() -> chatbotService.askStream(q, repeatCount, history, emitter,
                (finalAnswer, type) -> chatMessageRepository.save(new ChatMessage(sessionId, "assistant", finalAnswer, type)).getId()));

        // El frontend debe leer este header (fetch + ReadableStream, no sirve con EventSource nativo
        // porque el endpoint es POST) y guardar el sessionId (sessionStorage) para reenviarlo en cada
        // siguiente pregunta de la misma conversación.
        return ResponseEntity.ok().header("X-Session-Id", sessionId).body(emitter);
    }

    private String resolveSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId.trim();
    }

    private List<Map<String, String>> loadHistory(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByIdAsc(sessionId);
        int from = Math.max(0, messages.size() - MAX_HISTORY_MESSAGES);
        List<Map<String, String>> history = new ArrayList<>();
        for (ChatMessage m : messages.subList(from, messages.size())) {
            history.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        return history;
    }

    private int parseRepeatCount(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Math.max(0, Math.min(10, Integer.parseInt(value.trim())));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    @GetMapping("/faqs")
    public List<Map<String, Object>> faqs() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Faq faq : faqRepository.findByActivoTrueOrderByOrdenAsc()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("question", faq.getQuestion());
            item.put("answer", faq.getAnswer());
            result.add(item);
        }
        return result;
    }

    @PostMapping("/contact")
    public ResponseEntity<Map<String, Object>> contact(@RequestBody Map<String, String> body) {
        String nombre = body.getOrDefault("nombre", "").trim();
        String cedula = body.getOrDefault("cedula", "").trim();
        String correo = body.getOrDefault("correo", "").trim();
        String telefono = body.getOrDefault("telefono", "").trim();
        String area = body.getOrDefault("area", "").trim();
        String tipo = body.getOrDefault("tipo", "SOLICITAR INFORMACION").trim();
        String descripcion = body.getOrDefault("descripcion", "").trim();

        // Validación de longitud básica para evitar abuso
        if (nombre.length() > 120 || cedula.length() > 20 || correo.length() > 120 || telefono.length() > 20) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "Algunos campos exceden la longitud permitida."));
        }
        if (descripcion.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "La descripción no puede superar los 500 caracteres."));
        }

        // Validación de nombre (solo letras, espacios, tildes y caracteres básicos)
        if (!nombre.matches("^[A-Za-zÀ-ÿñÑ\\s'.-]{3,120}$")) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "El nombre completo no es válido. Solo se permiten letras."));
        }

        // Validación de cédula (solo números, 4-12 dígitos) - igual patrón del registro
        if (!cedula.matches(CEDULA_REGEX)) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "La cédula no es válida. Debe contener solo números (4-12 dígitos)."));
        }

        // Validación de correo
        if (!correo.matches(EMAIL_REGEX)) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "El correo no tiene un formato válido."));
        }

        // Validación de teléfono (mismo patrón del registro: 7-20 caracteres con +, dígitos, espacios, paréntesis, guiones)
        if (telefono.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "El teléfono es obligatorio."));
        }
        if (!telefono.matches(PHONE_REGEX)) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "El teléfono no es válido. Solo se permiten números, +, espacios, paréntesis y guiones (7-20 caracteres)."));
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setNombreCompleto(nombre);
        ticket.setNumeroDocumento(cedula);
        ticket.setTipoDocumento("CC");
        ticket.setEmail(correo);
        ticket.setTelefono(telefono);
        ticket.setDepartamento(area.isEmpty() ? "General" : area);
        ticket.setTipoPeticion(tipo);
        ticket.setSubject("Chatbot: " + tipo + " - " + area);
        ticket.setMessage("Solicitud enviada desde el chatbot.\nNombre: " + nombre +
                "\nCédula: " + cedula + "\nCorreo: " + correo +
                "\nTeléfono: " + telefono +
                "\nÁrea: " + (area.isEmpty() ? "No indicada" : area) +
                "\nTipo: " + tipo +
                (descripcion.isEmpty() ? "" : "\nDescripción: " + descripcion));
        ticket.setUsername(nombre);
        ticket.setOrigen("CHATBOT");
        ticket.setStatus("ABIERTO");
        supportTicketRepository.save(ticket);

        return ResponseEntity.ok(Map.of(
            "ok", "true",
            "message", "¡Gracias " + nombre + "! Tu solicitud (" + tipo + ") fue enviada al área de " + area +
                    ". Un representante te contactará pronto."));
    }

    @PostMapping("/rating")
    public ResponseEntity<Map<String, Object>> rating(@RequestBody Map<String, String> body) {
        String ratingStr = body.getOrDefault("rating", "").trim();
        String comment = body.getOrDefault("comment", "").trim();

        int rating;
        try {
            rating = Integer.parseInt(ratingStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "Calificación no válida."));
        }
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "La calificación debe estar entre 1 y 5."));
        }
        if (comment.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "El comentario es demasiado largo."));
        }

        chatbotRatingRepository.save(new ChatbotRating(rating, comment));
        return ResponseEntity.ok(Map.of(
            "ok", "true",
            "message", "¡Gracias por tu calificación! Nos ayuda a mejorar."));
    }

    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> feedback(@RequestBody Map<String, Object> body) {
        String sessionId = asString(body.get("sessionId"));
        Object msgIdObj = body.get("messageId");
        String feedback = asString(body.get("feedback"));
        if (sessionId == null || sessionId.isBlank() || msgIdObj == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", "false", "message", "Sesión o mensaje no válido."));
        }
        if (!"up".equals(feedback) && !"down".equals(feedback)) {
            return ResponseEntity.badRequest().body(Map.of("ok", "false", "message", "Votación no válida."));
        }
        long messageId;
        try {
            messageId = Long.parseLong(msgIdObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", "false", "message", "Mensaje no válido."));
        }

        ChatMessage message = chatMessageRepository.findById(messageId).orElse(null);
        if (message == null || !sessionId.equals(message.getSessionId()) || !"assistant".equals(message.getRole())) {
            return ResponseEntity.badRequest().body(Map.of("ok", "false", "message", "Mensaje no encontrado en esta sesión."));
        }

        message.setFeedback(feedback);
        chatMessageRepository.save(message);
        return ResponseEntity.ok(Map.of("ok", "true", "message", "Gracias por tu retroalimentación."));
    }
}
