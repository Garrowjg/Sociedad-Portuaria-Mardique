package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
public class ApiChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @PostMapping("/ask")
    public Map<String, String> ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return Map.of("answer", "Escribe una pregunta para poder ayudarte.");
        }
        String answer = chatbotService.ask(question);
        return Map.of("answer", answer);
    }
}
