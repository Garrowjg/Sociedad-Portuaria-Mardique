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
        // Full-text search
        List<KnowledgeChunk> results = knowledgeChunkRepository.searchByText(question, 8);
        // Fallback: search each significant word with ILIKE
        if (results.isEmpty()) {
            String[] words = question.toLowerCase().split("\\s+");
            java.util.Set<Long> seen = new java.util.HashSet<>();
            for (String word : words) {
                if (word.length() >= 3) {
                    List<KnowledgeChunk> wordResults = knowledgeChunkRepository.searchByLike(word, 5);
                    for (KnowledgeChunk kc : wordResults) {
                        if (!seen.contains(kc.getId())) {
                            results.add(kc);
                            seen.add(kc.getId());
                        }
                    }
                }
            }
        }
        if (results.isEmpty()) {
            return "";
        }
        return results.stream()
                .map(kc -> kc.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    private String buildSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres el asistente virtual de Sociedad Portuaria Mardique S.A.\n\n");
        sb.append("REGLAS ESTRICTAS:\n");
        sb.append("- Responde EN MÁXIMO 3-4 líneas.\n");
        sb.append("- Sé directo y preciso. No des listas largas ni párrafos enormes.\n");
        sb.append("- Usa negritas para datos clave (teléfonos, correos, nombres).\n");
        sb.append("- Si la pregunta es sobre algo que no tienes en la información, di: 'No tengo esa información, comunícate al [teléfono/correo de contacto].'\n");
        sb.append("- Nunca inventes información. Si no lo sabes, dilo.\n");
        sb.append("- Usa un tono amable pero profesional.\n\n");
        if (!context.isEmpty()) {
            sb.append("INFORMACIÓN DE LA EMPRESA:\n").append(context).append("\n\n");
            sb.append("Usa ÚNICAMENTE esta información para responder. No agregues información que no esté aquí.");
        } else {
            sb.append("No hay información específica para esta pregunta. Sugiere contactar a la empresa directamente.");
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
                "temperature", 0.5,
                "max_tokens", 300
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
