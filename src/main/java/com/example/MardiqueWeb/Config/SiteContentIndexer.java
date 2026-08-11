package com.example.MardiqueWeb.Config;

import com.example.MardiqueWeb.Service.SiteContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Indexa el texto de las páginas públicas del sitio en la base de conocimiento
 * del chatbot al arrancar la aplicación, para que siempre tenga la info visible
 * en la web (y no solo la de PDFs subidos manualmente).
 *
 * Se puede desactivar con chatbot.index-pages-at-startup=false.
 */
@Component
public class SiteContentIndexer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SiteContentIndexer.class);

    @Autowired
    private SiteContentService siteContentService;

    @Value("${chatbot.index-pages-at-startup:true}")
    private boolean indexAtStartup;

    @Override
    public void run(String... args) {
        if (!indexAtStartup) {
            log.info("Indexación de páginas del sitio al arranque desactivada");
            return;
        }
        try {
            int chunks = siteContentService.indexAllPages();
            log.info("Páginas del sitio indexadas para el chatbot: {} fragmentos", chunks);
        } catch (Exception e) {
            log.warn("No se pudieron indexar las páginas del sitio al arranque: {}", e.getMessage());
        }
    }
}