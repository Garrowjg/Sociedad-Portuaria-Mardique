package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.Faq;
import com.example.MardiqueWeb.Entity.KnowledgeChunk;
import com.example.MardiqueWeb.Repository.FaqRepository;
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

    public Map<String, Object> ask(String question) {
        // 1. Fuera de tema (medicina, deportes, etc.)
        if (isOffTopic(question)) {
            return Map.of("answer", OFF_TOPIC_MSG, "form", false);
        }

        // 2. Intención de contacto / pedir datos privados de alguien -> mostrar formulario
        if (isContactIntent(question)) {
            return Map.of("answer", CONTACT_MSG, "form", true);
        }

        // 3. Respuesta exacta de FAQ si existe
        Faq faq = findFaqMatch(question);
        if (faq != null) {
            return Map.of("answer", faq.getAnswer(), "form", false);
        }

        // 4. Consulta normal con la base de conocimiento
        String context = findRelevantContext(question);
        String systemPrompt = buildSystemPrompt(context);
        String answer = callGroq(systemPrompt, question);
        return Map.of("answer", answer, "form", false);
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
                .map(kc -> kc.getContent())
                .collect(Collectors.joining("\n\n"));
    }

    private String buildSystemPrompt(String context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres el asistente virtual de Sociedad Portuaria Mardique S.A., un puerto multipropósito ubicado en Cartagena, Colombia.\n\n");
        sb.append("REGLAS ESTRICTAS:\n");
        sb.append("- Responde EN MÁXIMO 3-4 líneas.\n");
        sb.append("- Sé directo y preciso. No des listas largas ni párrafos enormes.\n");
        sb.append("- Usa negritas para datos clave (teléfonos, correos, nombres).\n");
        sb.append("- Si la pregunta es sobre algo que no tienes en la información, di: 'No tengo esa información, comunícate al (57) 669 0730 o info@spmardique.com.'\n");
        sb.append("- Nunca inventes información. Si no lo sabes, dilo.\n");
        sb.append("- Usa un tono amable pero profesional.\n\n");
        sb.append("- IMPORTANTE: Tu enfoque es ÚNICAMENTE temas portuarios, logísticos y de la empresa Mardique. " +
                "Si te preguntan por temas ajenos (medicina, deportes, política, recetas, etc.), responde exactamente: " +
                "'Lo siento, mi enfoque es ayudarte únicamente con los temas relacionados con la Sociedad Portuaria Mardique y sus servicios portuarios, comerciales y logísticos. ¿Tienes alguna duda sobre nuestros servicios, tarifas, trámites o cómo contactarnos?'\n\n");
        sb.append("- PRIVACIDAD: NUNCA reveles números de teléfono personales, correos electrónicos personales ni datos de contacto directo de empleados (gerentes, representantes, etc.). " +
                "Es información privada. Si el usuario pide el número o correo de una persona, responde: " +
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
}