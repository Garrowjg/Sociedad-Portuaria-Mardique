package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.ChatbotRating;
import com.example.MardiqueWeb.Entity.Faq;
import com.example.MardiqueWeb.Entity.SupportTicket;
import com.example.MardiqueWeb.Repository.ChatbotRatingRepository;
import com.example.MardiqueWeb.Repository.FaqRepository;
import com.example.MardiqueWeb.Repository.SupportTicketRepository;
import com.example.MardiqueWeb.Service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^[0-9+\\-\\s()]{7,20}$";
    private static final String CEDULA_REGEX = "^[0-9]{4,12}$";

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return Map.of("answer", "Escribe una pregunta para poder ayudarte.", "form", false);
        }
        if (question.length() > 500) {
            return Map.of("answer", "Tu pregunta es muy larga. Intenta resumirla en máximo 500 caracteres.", "form", false);
        }
        return chatbotService.ask(question);
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

        // Validación de longitud básica para evitar abuso
        if (nombre.length() > 120 || cedula.length() > 20 || correo.length() > 120 || telefono.length() > 20) {
            return ResponseEntity.badRequest().body(Map.of(
                "ok", "false",
                "message", "Algunos campos exceden la longitud permitida."));
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
                "\nTipo: " + tipo);
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
}
