package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.KnowledgeChunk;
import com.example.MardiqueWeb.Repository.KnowledgeChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae el texto visible de las páginas públicas del sitio (templates HTML) y lo
 * indexa en la base de conocimiento (knowledge_chunks) para que el chatbot también
 * disponga de la información que los usuarios ven en la web y pueda guiarlos.
 *
 * Se ejecuta al arrancar la aplicación y cada vez que se guarda/actualiza el texto
 * de una página desde el panel del chatbot. Solo indexa las páginas públicas.
 */
@Service
public class SiteContentService {

    private static final Logger log = LoggerFactory.getLogger(SiteContentService.class);

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 100;

    // Páginas públicas cuyo texto visible se indexa. El nombre de fuente queda como "Página: {ruta}".
    private static final String[] PUBLIC_PAGES = {
            "Inicio", "Empresa", "Servicios", "Procedimientos",
            "Tramitesenlinea", "Tarifas", "Contacto", "Galeria"
    };

    private static final Pattern TAG = Pattern.compile("<[^>]*>");
    private static final Pattern SCRIPT_OR_STYLE = Pattern.compile("(?is)<(script|style)[^>]*>.*?</\\1>");
    private static final Pattern COMMENTS = Pattern.compile("(?s)<!--.*?-->");
    private static final Pattern THYMELEAF_ATTR = Pattern.compile("\\s+th:[a-z-]+=\"[^\"]*\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern THYMELEAF_INLINE = Pattern.compile(
            "\\$\\{[^}]*\\}|\\[\\[[^\\]]*\\]\\]|\\[\\([^)]*\\)\\]|th:[a-z0-9-]+=.{0,60}", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("[\\t\\r\\n ]+");
    private static final Pattern ENTITY = Pattern.compile("&(nbsp|amp|quot|apos|lt|gt|#39|#160);");

    /** Devuelve el texto visible de todas las páginas públicas, con la ruta como clave. */
    public Map<String, String> extractAllPages() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String page : PUBLIC_PAGES) {
            String text = extractTemplateText(page);
            if (text != null && !text.isBlank()) {
                result.put("/" + page.toLowerCase().replace("enlinea", "-en-linea"), text);
            }
        }
        return result;
    }

    /**
     * Indexa (reemplaza) el texto de todas las páginas públicas en la base de conocimiento.
     * Devuelve el número total de fragmentos generados.
     */
    public int indexAllPages() {
        Map<String, String> pages = extractAllPages();
        int total = 0;
        for (Map.Entry<String, String> e : pages.entrySet()) {
            total += processText(e.getValue(), "Página: " + e.getKey());
        }
        log.info("Sitio web indexado: {} páginas, {} fragmentos", pages.size(), total);
        return total;
    }

    /** Extrae el texto visible de una página concreta ("Inicio", "Tarifas", ...) o null si falla. */
    public String extractTemplateText(String page) {
        try {
            Resource resource = resolveTemplate(page);
            if (resource == null || !resource.exists()) {
                log.warn("Template de página no encontrado: {}", page);
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return htmlToText(sb.toString());
        } catch (Exception e) {
            log.error("Error extrayendo texto de la página {}: {}", page, e.getMessage());
            return null;
        }
    }

    private Resource resolveTemplate(String page) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            // Preferir el template del classpath de producción; en dev se lee de resources/templates.
            List<Resource> candidates = new ArrayList<>();
            Resource[] fromClasspath = resolver.getResources("classpath*:/templates/" + page + ".html");
            for (Resource r : fromClasspath) candidates.add(r);
            Path devPath = Path.of("src/main/resources/templates/" + page + ".html");
            if (Files.exists(devPath)) {
                candidates.add(new org.springframework.core.io.FileSystemResource(devPath.toFile()));
            }
            for (Resource r : candidates) {
                if (r.exists() && r.isReadable()) return r;
            }
            return candidates.isEmpty() ? null : candidates.get(0);
        } catch (Exception e) {
            log.error("Error resolviendo template {}: {}", page, e.getMessage());
            return null;
        }
    }

    /** Convierte HTML a texto plano visible: elimina scripts, estilos, atributos Thymeleaf y etiquetas. */
    String htmlToText(String html) {
        if (html == null || html.isEmpty()) return "";
        String text = SCRIPT_OR_STYLE.matcher(html).replaceAll(" ");
        text = COMMENTS.matcher(text).replaceAll(" ");
        text = THYMELEAF_ATTR.matcher(text).replaceAll("");
        text = THYMELEAF_INLINE.matcher(text).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = ENTITY.matcher(text).replaceAll(m -> switch (m.group(1)) {
            case "nbsp" -> " ";
            case "amp" -> "&";
            case "quot" -> "\"";
            case "apos" -> "'";
            case "lt" -> "<";
            case "gt" -> ">";
            case "#39" -> "'";
            case "#160" -> " ";
            default -> " ";
        });
        text = WHITESPACE.matcher(text).replaceAll(" ");
        return text.trim();
    }

    /** Reparte el texto en fragmentos y reemplaza los existentes para esa fuente. */
    int processText(String text, String source) {
        knowledgeChunkRepository.deleteBySource(source);
        List<String> chunks = chunkText(text);
        List<KnowledgeChunk> entities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk kc = new KnowledgeChunk(chunks.get(i), source);
            kc.setSection("Parte " + (i + 1));
            entities.add(kc);
        }
        knowledgeChunkRepository.saveAll(entities);
        return entities.size();
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;
        text = text.replaceAll("\\s+", " ").trim();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf(". ", end);
                if (lastPeriod > start + CHUNK_SIZE / 2) {
                    end = lastPeriod + 1;
                }
            }
            chunks.add(text.substring(start, end).trim());
            start = end - CHUNK_OVERLAP;
            if (start >= text.length() - CHUNK_OVERLAP) break;
        }
        return chunks;
    }
}