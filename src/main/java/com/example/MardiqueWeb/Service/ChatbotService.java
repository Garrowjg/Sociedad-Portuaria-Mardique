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

    @Value("${groq.model:openai/gpt-oss-120b}")
    private String groqModel;

    @Value("${groq.model-fallback:openai/gpt-oss-20b}")
    private String groqModelFallback;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Autowired
    private FaqRepository faqRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

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
            "maniobras", "seguimiento", "carga", "descarga", "cliente",
            // Comercio exterior / import-export
            "exportacion", "exportaciones", "exportar", "exportan", "importacion", "importaciones",
            "importar", "importan", "comercio exterior", "mercancia", "mercancias", "comprador",
            "vendedor", "trasbordo", "trasbordos", "cabotaje", "cabotajes", "multimodal",
            // Tipos de carga / graneles agrícolas y minerales que maneja el puerto
            "maiz", "cereal", "cereales", "soya", "trigo", "azucar", "carbon", "clinker", "cemento",
            "acero", "fertilizante", "fertilizantes", "combustible", "combustibles", "petroleo",
            "crudo", "crudos", "gas", "quimico", "quimicos", "mineral", "minerales", "biodiesel",
            "melaza", "arroz",
            // Infraestructura y operación
            "silo", "silos", "almacenamiento", "almacenaje", "bodega", "bodegas", "patio", "patios",
            "terminal", "terminales", "navegacion", "atraque", "fondeo", "practico", "practicaje",
            "pilotaje", "shore base", "rio magdalena", "canal del dique", "bahia de cartagena",
            "cormagdalena", "cardique", "concesion", "licencia ambiental", "capacidad", "toneladas",
            "empleados", "trabajadores", "sostenibilidad", "seguridad industrial", "medio ambiente"
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

    // Sobrecargas sin historial, por compatibilidad con llamadas existentes desde el controller.
    public Map<String, Object> ask(String question, int repeatCount) {
        return ask(question, repeatCount, List.of());
    }

    public void askStream(String question, int repeatCount, SseEmitter emitter) {
        askStream(question, repeatCount, List.of(), emitter, answer -> {});
    }

    /**
     * @param history mensajes previos de la conversación en orden cronológico, cada uno como
     *                Map.of("role", "user"|"assistant", "content", "..."). Es lo que permite que
     *                el modelo recuerde de qué se viene hablando y no repita siempre lo mismo
     *                ante preguntas de seguimiento o reformuladas.
     */
    public Map<String, Object> ask(String question, int repeatCount, List<Map<String, String>> history) {
        int tier = Math.min(Math.max(repeatCount, 0), 2);

        // 1. Saludo / conversación casual -> la IA responde normalmente
        if (isGreeting(question)) {
            String context = findRelevantContext(question);
            String answer = callGroq(buildSystemPrompt(context, tier), question, history);
            return Map.of("answer", answer, "form", false, "blocked", false);
        }

        // 2. Intención de contacto / pedir datos privados de alguien -> mostrar formulario
        if (isContactIntent(question)) {
            return Map.of("answer", CONTACT_REFUSAL_TIERS[tier], "form", true, "blocked", true);
        }

        // 3. Respuesta exacta de FAQ si existe
        Faq faq = findFaqMatch(question);
        if (faq != null) {
            return Map.of("answer", sanitizeContext(faq.getAnswer()), "form", false, "blocked", false);
        }

        // 4. Consulta con la base de conocimiento
        String context = findRelevantContext(question);

        // 5. Solo se bloquea como "fuera de tema" si NO hay keyword relacionada
        //    Y TAMPOCO se encontró nada en la base de conocimiento. Antes se bloqueaba
        //    con solo la keyword, lo que generaba falsos "fuera de tema" en preguntas
        //    válidas mal redactadas.
        if (context.isEmpty() && isOffTopic(question)) {
            return Map.of("answer", OFF_TOPIC_TIERS[tier], "form", false, "blocked", true);
        }

        // 6. Siempre se deja responder a la IA (con o sin contexto), pasándole el historial
        //    real de la conversación para que pueda profundizar, dar ángulos nuevos y no
        //    repetir la misma respuesta enlatada ante preguntas parecidas.
        String systemPrompt = buildSystemPrompt(context, tier);
        String answer = callGroq(systemPrompt, question, history);
        return Map.of("answer", sanitizeContext(answer), "form", false, "blocked", context.isEmpty());
    }

    public void askStream(String question, int repeatCount, List<Map<String, String>> history, SseEmitter emitter) {
        askStream(question, repeatCount, history, emitter, answer -> {});
    }

    /**
     * @param onComplete se invoca exactamente una vez, al terminar, con el texto final que el
     *                    usuario vio (ya sea el mensaje fijo de saludo/contacto/FAQ/fuera-de-tema,
     *                    o el texto acumulado del streaming de la IA). Pensado para que el
     *                    controller pueda persistir el turno completo en base de datos sin tener
     *                    que reconstruirlo token a token.
     */
    public void askStream(String question, int repeatCount, List<Map<String, String>> history, SseEmitter emitter,
                           java.util.function.Consumer<String> onComplete) {
        int tier = Math.min(Math.max(repeatCount, 0), 2);
        try {
            if (isGreeting(question)) {
                streamGroq(buildSystemPrompt(findRelevantContext(question), tier), question, history, emitter, false, false, onComplete);
                return;
            }
            if (isContactIntent(question)) {
                String text = CONTACT_REFUSAL_TIERS[tier];
                sendEvent(emitter, text, true, true, onComplete);
                return;
            }
            Faq faq = findFaqMatch(question);
            if (faq != null) {
                String text = sanitizeContext(faq.getAnswer());
                sendEvent(emitter, text, false, false, onComplete);
                return;
            }
            String context = findRelevantContext(question);
            if (context.isEmpty() && isOffTopic(question)) {
                String text = OFF_TOPIC_TIERS[tier];
                sendEvent(emitter, text, false, true, onComplete);
                return;
            }
            streamGroq(buildSystemPrompt(context, tier), question, history, emitter, false, context.isEmpty(), onComplete);
        } catch (Exception e) {
            log.error("Stream error: {}", e.getMessage(), e);
            try {
                String text = "Lo siento, ocurrió un error al procesar tu consulta.";
                sendEvent(emitter, text, false, false, onComplete);
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

    private String buildSystemPrompt(String context, int tier) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres el asistente virtual de Sociedad Portuaria Mardique S.A., un puerto multipropósito privado de uso público ubicado en Cartagena, Colombia.\n\n");
        sb.append("REGLAS DE ESTILO:\n");
        sb.append("- Adapta la extensión a la pregunta: para datos puntuales o saludos, 2-4 líneas; si el usuario pide que lo convenzas, que profundices o compares, puedes usar hasta 6-8 líneas o una lista corta con viñetas. Nunca sacrifiques información útil solo por acortar.\n");
        sb.append("- Sé directo y concreto, evita relleno y frases de relleno vacías.\n");
        sb.append("- Usa negritas para datos clave (nombres de áreas, cifras, ubicaciones).\n");
        sb.append("- Si el usuario te saluda (hola, buenos días, ¿cómo estás?) responde de forma amable y natural, preséntate brevemente y pregunta en qué le puedes ayudar. No des respuestas de máquina.\n");
        sb.append("- Nunca inventes información que no esté en este prompt, en el contexto entregado o en las FAQ. Si no tienes el dato exacto, dilo con naturalidad y ofrece una alternativa (contacto, formulario).\n");
        sb.append("- Usa un tono amable, cercano y profesional, como una persona real del equipo de atención, no como un script.\n");
        sb.append("- MUY IMPORTANTE - VARÍA TUS RESPUESTAS: tienes abajo el HISTORIAL DE LA CONVERSACIÓN. Si el usuario pregunta algo parecido a lo que ya preguntó o insiste en el mismo tema con otras palabras, NO repitas la misma respuesta ni la misma estructura de frases. Aporta un ángulo, dato o ejemplo nuevo que no hayas mencionado antes en esta conversación, o profundiza un nivel más de detalle. Si de verdad no tienes nada nuevo que agregar, dilo con honestidad en vez de reciclar el mismo texto.\n");
        sb.append("- Si el usuario pide que 'lo convenzas' de algo o pregunta '¿qué tan bueno es Mardique?', no repitas siempre la misma lista de servicios: elige un enfoque distinto cada vez (por ejemplo: ubicación estratégica y conectividad fluvial, experiencia operando cargas de gran volumen, capacidad de silos y almacenamiento, seguridad y cumplimiento ambiental, cifras de crecimiento, casos reales de operación).\n\n");

        sb.append("EJEMPLOS DE CÓMO CONVERSAR (no los copies literal, son solo guía de tono):\n");
        sb.append("Usuario: hola, buenas\n");
        sb.append("Asistente: ¡Hola! Bienvenido a Mardique 👋 ¿En qué te puedo ayudar hoy? Puedo darte info sobre servicios, trámites o cómo contactarnos.\n\n");
        sb.append("Usuario: ¿qué servicios ofrecen?\n");
        sb.append("Asistente: Ofrecemos manejo de carga general, contenedores y graneles, además de servicios portuarios y logísticos asociados. ¿Buscas info de algún servicio en particular?\n\n");
        sb.append("Usuario: necesito el celular del gerente\n");
        sb.append("Asistente: No comparto datos de contacto directo por política de la empresa, pero si agendas tu solicitud aquí mismo, un representante del área te contacta enseguida. ¿Te ayudo a dejar la solicitud?\n\n");
        sb.append("Usuario: ¿cuál es la capital de Francia?\n");
        sb.append("Asistente: Eso se sale un poco de lo mío 😅 Mi fuerte son los temas de Mardique: servicios portuarios, tarifas, trámites y contacto. ¿Te ayudo con algo de eso?\n\n");

        sb.append("DATOS REALES DE LA EMPRESA (puedes usarlos libremente, son públicos y verificados):\n");
        sb.append("- Mardique es un puerto multipropósito privado de uso público, con conexión directa al río Magdalena a través del Canal del Dique y a la Bahía de Cartagena, lo que la conecta con el interior del país y con el resto del mundo.\n");
        sb.append("- Es, por su posición estratégica, uno de los pocos puertos capaces de reducir trasbordos y cabotajes en operaciones de importación y exportación con conexión directa al interior de Colombia.\n");
        sb.append("- Servicios principales: grúas móviles; infraestructura para manejo y almacenaje de hidrocarburos (con autorización del Ministerio de Minas y Energía para almacenamiento de crudos y combustibles líquidos); silos para graneles sólidos; manejo de carga general y de proyecto; shore base; manipulación de contenedores; y operaciones de transporte terrestre y fluvial.\n");
        sb.append("- Mueve tanto graneles agrícolas (por ejemplo maíz amarillo, en operaciones de más de 50.000 toneladas por buque, a un ritmo de hasta 10.000 toneladas/día) como minerales e insumos para construcción (por ejemplo clinker a granel, en arribos de más de 60.000 toneladas).\n");
        sb.append("- Cuenta con concesión portuaria otorgada por Cormagdalena y licencia ambiental otorgada por Cardique.\n");
        sb.append("- Ubicada en Cartagena, Bolívar (corregimiento de Santa Ana / Isla de Barú, vía a Barú). Opera 24 horas, los 365 días del año; las oficinas administrativas atienden en horario hábil de lunes a viernes.\n");
        sb.append("- Misión: prestar servicios portuarios y logísticos con excelencia y ventajas competitivas para sus usuarios y clientes. Visión: ser la terminal más importante de Colombia en servicios portuarios y logísticos, mediante una plataforma logística multimodal que impulse el comercio exterior del país.\n\n");

        if (context.isEmpty()) {
            sb.append("SITUACIÓN ACTUAL: no se encontró información específica en la base de conocimiento para esta pregunta.\n");
            if (tier == 0) {
                sb.append("Dile de forma natural y breve que no tienes ese dato exacto en este momento, y sugiere comunicarse " +
                        "al (57) 669 0730 o info@spmardique.com. Si la pregunta claramente no tiene nada que ver con Mardique " +
                        "(temas ajenos como medicina, deportes, recetas, política, etc.), acláralo con amabilidad y redirige " +
                        "la conversación hacia en qué sí puedes ayudar.\n\n");
            } else if (tier == 1) {
                sb.append("Ya le dijiste antes que no tenías ese dato. Esta vez varía la redacción (no repitas la misma frase) " +
                        "y sugiere dejar su solicitud en el formulario del chat para que un representante del área lo confirme.\n\n");
            } else {
                sb.append("Es al menos el tercer intento con este mismo tema. Sé empático, varía la redacción de nuevo, y anímalo " +
                        "directamente a dejar la solicitud en el formulario ('¿te dejo el formulario listo?').\n\n");
            }
        }

        sb.append("- PRIVACIDAD (MUY IMPORTANTE): NUNCA reveles números de teléfono personales, correos electrónicos personales ni datos de contacto directo de empleados (gerentes, Oscar, representantes, personal). " +
                "Es información privada y está PROHIBIDO mencionar nombres de empleados junto con su teléfono o correo. " +
                "Si el usuario pide el número o correo de una persona o empleado, dile con tus palabras que no compartes esos datos " +
                "por política de la empresa, y que agendando una cita o dejando su solicitud, un representante lo atenderá.\n\n");
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
        }
        return sb.toString();
    }

    /**
     * Construye la lista de mensajes para Groq: system + historial real de la conversación
     * (recortado a los últimos turnos para no disparar el consumo de tokens) + el mensaje actual.
     * Pasar el historial es lo que le permite al modelo "recordar" de qué se viene hablando,
     * en vez de responder siempre la misma frase genérica a preguntas parecidas.
     */
    private List<Map<String, String>> buildMessages(String systemPrompt, String userMessage, List<Map<String, String>> history) {
        List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null && !history.isEmpty()) {
            int maxTurns = 12; // últimos ~12 mensajes (6 intercambios) para no saturar el contexto
            int from = Math.max(0, history.size() - maxTurns);
            for (Map<String, String> turn : history.subList(from, history.size())) {
                String role = turn.getOrDefault("role", "user");
                String content = turn.getOrDefault("content", "");
                if (!content.isBlank()) {
                    messages.add(Map.of("role", role, "content", content));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    private String callGroq(String systemPrompt, String userMessage, List<Map<String, String>> history) {
        String[] models = { groqModel, groqModelFallback };
        List<Map<String, String>> messages = buildMessages(systemPrompt, userMessage, history);
        for (String model : models) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(groqApiKey);

                Map<String, Object> requestBody = Map.of(
                        "model", model,
                        "messages", messages,
                        "temperature", 0.65,
                        "max_tokens", 600
                );

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

                if (!response.getStatusCode().is2xxSuccessful()) {
                    log.error("Groq API HTTP {} (model {}): {}", response.getStatusCode(), model, response.getBody());
                    continue;
                }

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
                log.error("Groq API error (model {}): {}", model, e.getMessage(), e);
            }
        }
        return "Lo siento, ocurrió un error al procesar tu consulta. Intenta de nuevo más tarde.";
    }

    private void sendEvent(SseEmitter emitter, String text, boolean form, boolean blocked, java.util.function.Consumer<String> onComplete) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("token", text);
            payload.put("form", form);
            payload.put("blocked", blocked);
            emitter.send(SseEmitter.event().name("message").data(payload));
            emitter.send(SseEmitter.event().name("done").data(Map.of("done", true)));
            emitter.complete();
            onComplete.accept(text);
        } catch (Exception e) {
            log.error("SSE send error: {}", e.getMessage());
            emitter.complete();
        }
    }

    private void streamGroq(String systemPrompt, String userMessage, List<Map<String, String>> history, SseEmitter emitter,
                             boolean form, boolean blocked, java.util.function.Consumer<String> onComplete) {
        try {
            boolean ok = attemptStream(systemPrompt, userMessage, history, emitter, form, blocked, groqModel, onComplete);
            if (!ok) {
                log.warn("Groq stream falló con el modelo principal '{}', reintentando con '{}'", groqModel, groqModelFallback);
                ok = attemptStream(systemPrompt, userMessage, history, emitter, form, blocked, groqModelFallback, onComplete);
            }
            if (!ok) {
                String text = "No pude conectarme con el servicio de IA en este momento. Intenta de nuevo.";
                sendEvent(emitter, text, false, false, onComplete);
            }
        } catch (Exception e) {
            log.error("Groq stream error: {}", e.getMessage(), e);
            try {
                String text = "Lo siento, ocurrió un error al procesar tu consulta. Intenta de nuevo más tarde.";
                sendEvent(emitter, text, false, false, onComplete);
            } catch (Exception ex) {
                emitter.complete();
            }
        }
    }

    /**
     * Intenta una solicitud de streaming a Groq con el modelo indicado.
     * Devuelve false solo si la respuesta HTTP no fue 2xx (antes de emitir tokens),
     * para que el llamador pueda reintentar con otro modelo sin duplicar contenido.
     * Errores a mitad de stream se propagan como excepción (no se reintenta).
     */
    private boolean attemptStream(String systemPrompt, String userMessage, List<Map<String, String>> history,
                                  SseEmitter emitter, boolean form, boolean blocked, String model,
                                  java.util.function.Consumer<String> onComplete) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> messages = buildMessages(systemPrompt, userMessage, history);
        String requestBody = mapper.writeValueAsString(Map.of(
                "model", model,
                "messages", messages,
                "temperature", 0.65,
                "max_tokens", 600,
                "stream", true
        ));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(GROQ_URL))
                .header("Authorization", "Bearer " + groqApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 300) {
            String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
            log.error("Groq HTTP {} (model {}): {}", response.statusCode(), model, errBody);
            return false;
        }

        StringBuilder fullAnswer = new StringBuilder();
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
                    String token = sanitizeContext(delta.asText());
                    fullAnswer.append(token);
                    Map<String, Object> payload = new java.util.LinkedHashMap<>();
                    payload.put("token", token);
                    payload.put("form", form);
                    payload.put("blocked", blocked);
                    emitter.send(SseEmitter.event().name("message").data(payload));
                }
            } catch (Exception ignore) {
            }
        }
        emitter.send(SseEmitter.event().name("done").data(Map.of("done", true)));
        emitter.complete();
        onComplete.accept(fullAnswer.toString());
        return true;
    }
}