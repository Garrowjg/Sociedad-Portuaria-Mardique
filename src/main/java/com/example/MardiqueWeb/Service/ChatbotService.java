package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.Faq;
import com.example.MardiqueWeb.Entity.KnowledgeChunk;
import com.example.MardiqueWeb.Repository.FaqRepository;
import com.example.MardiqueWeb.Repository.KnowledgeChunkRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    @Value("${groq.api-key}")
    private String groqApiKey;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Autowired
    private FaqRepository faqRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private static final String OFF_TOPIC_MSG =
            "Lo siento, mi enfoque es ayudarte únicamente con los temas relacionados con la Sociedad Portuaria Mardique y sus servicios portuarios, comerciales y logísticos. " +
                    "¿Tienes alguna duda sobre nuestros servicios, tarifas, trámites o cómo contactarnos?";

    private static final String CONTACT_MSG =
            "Para contactar al área o persona que necesitas, **agenda una cita o solicita información** y un representante de Mardique te atenderá. " +
                    "Por favor completa el siguiente formulario y te contactaremos a la brevedad.";

    // Respuestas escalonadas ante peticiones repetidas (índice 0 = 1er intento)
    private static final String[] CONTACT_REFUSAL_TIERS = {
            CONTACT_MSG,
            "Entiendo que necesitas comunicarte con el área o persona indicada. Por políticas de atención al cliente " +
                    "no compartimos números directos, pero si **agendas una cita o dejas tu solicitud** en el formulario, " +
                    "un representante te contactará a la brevedad.",
            "Entiendo perfectamente que necesitas comunicarte lo antes posible. Por políticas de atención, y para " +
                    "garantizarte una asesoría dedicada y sin esperas, no compartimos números directos. Sin embargo, " +
                    "agendando tu cita aquí mismo, un representante te atenderá sin filas. **¿Te gustaría agendar tu cita ahora?**"
    };

    // Respuestas escalonadas cuando no hay información disponible (precios, datos internos, etc.)
    private static final String[] NO_INFO_TIERS = {
            "No tengo esa información disponible en este momento. Para más detalles, comunícate al **(57) 669 0730** o escríbenos a **info@spmardique.com**.",
            "No tengo ese dato en mi base. Te recomiendo **solicitar información** a través del formulario del chat y un " +
                    "representante del área correspondiente te lo confirmará con precisión.",
            "Lamento no poder darte ese dato exacto por ahora, es información que maneja directamente el área responsable. " +
                    "La forma más rápida de obtenerlo con precisión es **dejando tu solicitud aquí mismo** y te responderán " +
                    "en breve. **¿Prefieres que te deje el formulario listo?**"
    };

    // Respuestas escalonadas ante temas fuera del alcance repetidos
    private static final String[] OFF_TOPIC_TIERS = {
            OFF_TOPIC_MSG,
            "Como te comenté, mi función es atender temas de la Sociedad Portuaria Mardique y sus servicios " +
                    "portuarios, comerciales y logísticos. ¿Te ayudo con algo relacionado, por ejemplo tarifas, trámites o cómo contactarnos?",
            "Entiendo que tengas esa duda, pero no es un tema que pueda atender aquí. Mi especialidad son los servicios " +
                    "de Mardique. Para no hacerte esperar, **¿te gustaría que te ayude con servicios, tarifas o trámites?**"
    };

    // Palabras clave relacionadas con Mardique / sector portuario
    private static final String[] RELATED_KEYWORDS = {
            "puerto", "portuaria", "mardique", "servicio", "servicios", "tarifa", "tarifas",
            "tramite", "tramites", "barco", "barcos", "carga", "cargas", "contenedor", "contenedores",
            "muelle", "muelles", "operacion", "operaciones", "aduana", "aduanera", "logistica",
            "embarque", "desembarque", "nave", "naves", "buque", "buques", "grua", "gruas", "granel",
            "graneles", "hidrocarburo", "hidrocarburos", "zona franca", "cliente", "clientes",
            "proveedor", "proveedores", "inscripcion", "comercial", "certificacion", "documento",
            "documentacion", "factura", "facturas", "pago", "pagos", "atracar", "remolque", "estiba",
            "transporte", "contacto", "contactar", "contactarnos", "telefono", "correo", "email",
            "ubicacion", "ubicados", "direccion", "horario", "horarios", "empleo", "pasantia",
            "pasantias", "trabajo", "vacante", "vacantes", "solicitud", "solicitudes", "queja",
            "reclamo", "peticion", "pqrs", "atencion", "atender", "gerente", "gerencia", "linea",
            "lineas", "comunicarme", "cita", "reunion", "agendar", "tarifario", "naviero", "naviera",
            "empresa", "compania", "spmardique", "informacion", "servicios", "zona", "franca",
            "calado", "eslora", "manga", "registro", "requisitos", "cotizacion", "cotizar", "maniobra",
            "maniobras", "seguimiento", "carga", "descarga", "cliente"
    };

    private static final String[] CONTACT_INTENT_KEYWORDS = {
            "contactar", "contacto", "contactarnos", "comunicarme", "hablar con", "atender",
            "atencion", "agendar", "cita", "citas", "reunion", "reuniones", "telefono", "telefono de",
            "numero", "numero de", "numero del", "correo", "correo de", "email", "email de",
            "gerente", "gerencia", "representante", "asesor", "asesoria", "solicitar informacion",
            "linea de atencion", "lineas de atencion", "informacion de contacto", "donde los contacto",
            "como los contacto", "como contacto", "medio de contacto", "atenderme", "contactar al",
            "hablar con el", "hablar con la", "con quien"
    };

    private static final String[] GREETING_KEYWORDS = {
            "hola", "buenas", "buenos dias", "buenas tardes", "buenas noches", "que tal", "que mas",
            "como estas", "como esta", "como te va", "hey", "saludos", "bienvenido", "bienvenida",
            "gusto", "mucho gusto", "encantado", "hello", "hi", "ey", "oye", "buen dia", "buenas"
    };

    public Map<String, Object> ask(String question, int repeatCount) {
        int tier = Math.min(Math.max(repeatCount, 0), 2);

        // 1. Saludo / conversación casual -> la IA responde normalmente
        if (isGreeting(question)) {
            String context = findRelevantContext(question);
            String answer = callGroq(buildSystemPrompt(context), question);
            return Map.of("answer", answer, "form", false, "blocked", false);
        }

        // 2. Fuera de tema (medicina, deportes, etc.)
        if (isOffTopic(question)) {
            return Map.of("answer", OFF_TOPIC_TIERS[tier], "form", false, "blocked", true);
        }

        // 3. Intención de contacto / pedir datos privados de alguien -> mostrar formulario
        if (isContactIntent(question)) {
            return Map.of("answer", CONTACT_REFUSAL_TIERS[tier], "form", true, "blocked", true);
        }

        // 4. Respuesta exacta de FAQ si existe
        Faq faq = findFaqMatch(question);
        if (faq != null) {
            return Map.of("answer", sanitizeContext(faq.getAnswer()), "form", false, "blocked", false);
        }

        // 5. Consulta con la base de conocimiento
        String context = findRelevantContext(question);
        if (context.isEmpty()) {
            return Map.of("answer", NO_INFO_TIERS[tier], "form", false, "blocked", true);
        }
        String systemPrompt = buildSystemPrompt(context);
        String answer = callGroq(systemPrompt, question);
        return Map.of("answer", sanitizeContext(answer), "form", false, "blocked", false);
    }

    public void askStream(String question, int repeatCount, SseEmitter emitter) {
        int tier = Math.min(Math.max(repeatCount, 0), 2);
        try {
            if (isGreeting(question)) {
                streamGroq(buildSystemPrompt(findRelevantContext(question)), question, emitter, false, false);
                return;
            }
            if (isOffTopic(question)) {
                sendEvent(emitter, OFF_TOPIC_TIERS[tier], false, true);
                return;
            }
            if (isContactIntent(question)) {
                sendEvent(emitter, CONTACT_REFUSAL_TIERS[tier], true, true);
                return;
            }
            Faq faq = findFaqMatch(question);
            if (faq != null) {
                sendEvent(emitter, sanitizeContext(faq.getAnswer()), false, false);
                return;
            }
            String context = findRelevantContext(question);
            if (context.isEmpty()) {
                sendEvent(emitter, NO_INFO_TIERS[tier], false, true);
                return;
            }
            streamGroq(buildSystemPrompt(context), question, emitter, false, false);
        } catch (Exception e) {
            log.error("Stream error: {}", e.getMessage(), e);
            try {
                sendEvent(emitter, "Lo siento, ocurrió un error al procesar tu consulta.", false, false);
            } catch (Exception ex) {
                emitter.complete();
            }
        }
    }

    private String normalize(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").trim();
    }

    private boolean isOffTopic(String question) {
        String q = normalize(question);
        if (q.length() < 3) return true;
        for (String keyword : RELATED_KEYWORDS) {
            if (q.contains(normalize(keyword))) return false;
        }
        return true;
    }

    private boolean isGreeting(String question) {
        String q = normalize(question);
        if (q.length() > 80) return false;
        for (String keyword : GREETING_KEYWORDS) {
            if (q.contains(normalize(keyword))) return true;
        }
        return false;
    }

    private boolean isContactIntent(String question) {
        String q = normalize(question);
        for (String keyword : CONTACT_INTENT_KEYWORDS) {
            if (q.contains(normalize(keyword))) return true;
        }
        return false;
    }

    private Faq findFaqMatch(String question) {
        String q = normalize(question);
        List<Faq> faqs = faqRepository.findByActivoTrueOrderByOrdenAsc();
        for (Faq faq : faqs) {
            String faqQ = normalize(faq.getQuestion());
            if (faqQ.isEmpty()) continue;
            // coincidencia exacta, contenida o muy similar
            if (q.equals(faqQ) || q.contains(faqQ) || faqQ.contains(q)) {
                if (q.length() >= 5 || faqQ.length() <= q.length() + 5) {
                    return faq;
                }
            }
        }
        return null;
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
                .map(kc -> sanitizeContext(kc.getContent()))
                .filter(c -> !c.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Elimina datos personales de empleados del contexto antes de enviarlo a la IA:
     * números de celular colombianos y correos personales.
     */
    private String sanitizeContext(String content) {
        if (content == null || content.isEmpty()) return "";
        String c = content;
        // Celulares colombianos: 3xx xxx xxxx (con/sin espacios, guiones o puntos)
        c = c.replaceAll("(?<!\\d)3\\d{2}\\s?\\d{3}\\s?\\d{4}(?!\\d)", "[número protegido]");
        c = c.replaceAll("(?<!\\d)3\\d{9}(?!\\d)", "[número protegido]");
        // Correos personales (no los institucionales públicos)
        c = c.replaceAll("[A-Za-z0-9._%+-]+@(?!spmardique\\.com\\b)[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[correo protegido]");
        // Teléfonos fijos personales con 8 dígitos que no sean el PBX
        c = c.replaceAll("(?<!\\d)(5)\\d{2}\\s?\\d{3}\\s?\\d{3}(?!\\d)", "[teléfono protegido]");
        return c;
    }

    private String buildSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres el asistente virtual de Sociedad Portuaria Mardique S.A., un puerto multipropósito ubicado en Cartagena, Colombia.\n\n");
        sb.append("REGLAS ESTRICTAS:\n");
        sb.append("- Responde EN MÁXIMO 3-4 líneas.\n");
        sb.append("- Sé directo y preciso. No des listas largas ni párrafos enormes.\n");
        sb.append("- Usa negritas para datos clave (nombres de áreas).\n");
        sb.append("- Si el usuario te saluda (hola, buenos días, ¿cómo estás?) responde de forma amable y natural, preséntate brevemente y pregunta en qué le puedes ayudar. No des respuestas de máquina ni repitas el mensaje de error.\n");
        sb.append("- Si la pregunta es sobre algo que no tienes en la información, di: 'No tengo esa información, comunícate al (57) 669 0730 o info@spmardique.com.'\n");
        sb.append("- Nunca inventes información. Si no lo sabes, dilo.\n");
        sb.append("- Usa un tono amable pero profesional.\n\n");
        sb.append("- IMPORTANTE: Tu enfoque principal son temas portuarios, logísticos y de la empresa Mardique. " +
                "Si te preguntan por temas ajenos (medicina, deportes, política, recetas, etc.), responde exactamente: " +
                "'Lo siento, mi enfoque es ayudarte únicamente con los temas relacionados con la Sociedad Portuaria Mardique y sus servicios portuarios, comerciales y logísticos. ¿Tienes alguna duda sobre nuestros servicios, tarifas, trámites o cómo contactarnos?'\n\n");
        sb.append("- PRIVACIDAD (MUY IMPORTANTE): NUNCA reveles números de teléfono personales, correos electrónicos personales ni datos de contacto directo de empleados (gerentes, Oscar, representantes, personal). " +
                "Es información privada y está PROHIBIDO mencionar nombres de empleados junto con su teléfono o correo. " +
                "Si el usuario pide el número o correo de una persona o empleado, responde: " +
                "'Para contactar al [área o persona que piden], agenda una cita o solicita información y un representante te atenderá.' " +
                "y dile que complete el formulario de contacto del chat.\n\n");
        sb.append("- Si el usuario pregunta cómo contactar, agendar una cita o reunión, o solicitar información, " +
                "indícale que complete el formulario de contacto que aparecerá en el chat y menciona brevemente que " +
                "un representante del área elegida lo atenderá.\n\n");

        // Agregar FAQs como contexto conocido
        List<Faq> faqs = faqRepository.findByActivoTrueOrderByOrdenAsc();
        if (!faqs.isEmpty()) {
            sb.append("PREGUNTAS FRECUENTES (respóndelas con estos datos):\n");
            for (Faq faq : faqs) {
                sb.append("- P: ").append(faq.getQuestion()).append("\n");
                sb.append("  R: ").append(faq.getAnswer()).append("\n");
            }
            sb.append("\n");
        }

        // Áreas de atención (SIN datos personales de empleados)
        sb.append("ÁREAS DE ATENCIÓN de la empresa (menciona su existencia si preguntan, pero NO reveles datos de contacto de personas):\n");
        sb.append("- Gerente Comercial\n");
        sb.append("- Representante Legal\n");
        sb.append("- Gerente de Operaciones\n");
        sb.append("- Gerencia Administrativa\n");
        sb.append("- Seguridad\n");
        sb.append("- Documentación Aduanera\n");
        sb.append("- Talento Humano\n");
        sb.append("- Contabilidad\n");
        sb.append("- Coordinación de Operaciones\n");
        sb.append("- Supervisor Zona Franca\n");
        sb.append("- Inscripción de Usuarios\n");
        sb.append("- Asistente Adm. y Compras\n\n");
        sb.append("Para contactar cualquier área, el usuario debe completar el formulario de contacto del chat.\n\n");

        if (!context.isEmpty()) {
            sb.append("INFORMACIÓN DE LA EMPRESA:\n").append(context).append("\n\n");
            sb.append("Usa ÚNICAMENTE esta información para responder. No agregues información que no esté aquí.");
        } else {
            sb.append("No hay información específica para esta pregunta en la base de conocimiento. " +
                    "Responde con lo que conozcas de Mardique (servicios, líneas de atención) o sugiere contactar " +
                    "al (57) 669 0730 o info@spmardique.com.");
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

    private void sendEvent(SseEmitter emitter, String text, boolean form, boolean blocked) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("token", text);
            payload.put("form", form);
            payload.put("blocked", blocked);
            emitter.send(SseEmitter.event().name("message").data(payload));
            emitter.send(SseEmitter.event().name("done").data(Map.of("done", true)));
            emitter.complete();
        } catch (Exception e) {
            log.error("SSE send error: {}", e.getMessage());
            emitter.complete();
        }
    }

    private void streamGroq(String systemPrompt, String userMessage, SseEmitter emitter, boolean form, boolean blocked) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(Map.of(
                    "model", MODEL,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.5,
                    "max_tokens", 300,
                    "stream", true
            ));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create(GROQ_URL))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if (data.equals("[DONE]")) break;
                try {
                    JsonNode node = mapper.readTree(data);
                    JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                    if (delta.isTextual() && !delta.asText().isEmpty()) {
                        Map<String, Object> payload = new java.util.LinkedHashMap<>();
                        payload.put("token", sanitizeContext(delta.asText()));
                        payload.put("form", form);
                        payload.put("blocked", blocked);
                        emitter.send(SseEmitter.event().name("message").data(payload));
                    }
                } catch (Exception ignore) {
                }
            }
            emitter.send(SseEmitter.event().name("done").data(Map.of("done", true)));
            emitter.complete();
        } catch (Exception e) {
            log.error("Groq stream error: {}", e.getMessage(), e);
            try {
                sendEvent(emitter, "Lo siento, ocurrió un error al procesar tu consulta. Intenta de nuevo más tarde.", false, false);
            } catch (Exception ex) {
                emitter.complete();
            }
        }
    }
}