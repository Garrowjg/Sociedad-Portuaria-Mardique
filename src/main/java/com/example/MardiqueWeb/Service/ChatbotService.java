package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.KnowledgeChunk;
import com.example.MardiqueWeb.Repository.KnowledgeChunkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    public String ask(String question) {
        String context = findRelevantContext(question);
        String systemPrompt = buildSystemPrompt(context);
        String answer = callGroq(systemPrompt, question);
        return answer;
    }

    private String findRelevantContext(String question) {
        List<KnowledgeChunk> results = knowledgeChunkRepository.searchByText(question, 5);
        if (results.isEmpty()) {
            results = knowledgeChunkRepository.searchByLike(question, 5);
        }
        if (results.isEmpty()) {
            return "";
        }
        return results.stream()
                .map(kc -> "[Fuente: " + kc.getSource() + " - " + kc.getSection() + "]\n" + kc.getContent())
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String buildSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres un asistente virtual de la Sociedad Portuaria Mardique S.A., un puerto marítimo en la región Caribe colombiana. ");
        sb.append("Responde preguntas sobre la empresa, sus servicios, trámites, tarifas, contacto y operaciones portuarias. ");
        sb.append("Sé amable, profesional y responde en español de forma clara y concisa.\n\n");
        if (!context.isEmpty()) {
            sb.append("A continuación tienes información relevante de la base de conocimientos para responder:\n\n");
            sb.append(context);
            sb.append("\n\nUsa esta información para responder. Si no encuentras la respuesta en la información proporcionada, ");
            sb.append("indica amablemente que no tienes esa información y sugiere contactar directamente con la empresa.");
        } else {
            sb.append("No hay información específica en la base de conocimientos para esta consulta. ");
            sb.append("Responde de forma general si es posible, o indica que no tienes la información y sugiere contactar con la empresa.");
        }
        return sb.toString();
    }

    private String callGroq(String systemPrompt, String userMessage) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey);

            Map<String, Object> requestBody = Map.of(
                "model", MODEL,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.7,
                "max_tokens", 1024
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

            Map body = response.getBody();
            if (body != null && body.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return "Lo siento, no pude procesar tu consulta en este momento.";
        } catch (Exception e) {
            log.error("Groq API error: {}", e.getMessage(), e);
            return "Lo siento, ocurrió un error al procesar tu consulta. Intenta de nuevo más tarde.";
        }
    }
}
