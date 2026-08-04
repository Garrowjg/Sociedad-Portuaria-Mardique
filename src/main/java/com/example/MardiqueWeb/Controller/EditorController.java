package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.Contact;
import com.example.MardiqueWeb.Entity.Document;
import com.example.MardiqueWeb.Entity.GalleryImage;
import com.example.MardiqueWeb.Entity.PageContent;
import com.example.MardiqueWeb.Entity.SystemConfig;
import com.example.MardiqueWeb.Repository.*;
import com.example.MardiqueWeb.Service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/editor")
public class EditorController {

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private GalleryImageRepository galleryImageRepository;

    @Autowired
    private PageContentRepository pageContentRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "gif", "webp", "svg", "doc", "docx", "xls", "xlsx");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "svg");

    private static final List<String> CONTENT_PAGES = List.of(
            "inicio", "empresa", "servicios", "procedimientos", "tramites", "galeria", "contacto", "tarifas");

    private static final Map<String, List<String>> PAGE_SECTION_KEYS = new LinkedHashMap<>();
    private static final Map<String, String> SECTION_LABELS = new LinkedHashMap<>();
    static {
        PAGE_SECTION_KEYS.put("inicio", List.of(
                "hero-title", "hero-sub", "hero-badge", "hero-cta-1", "hero-cta-2",
                "services-title",
                "service-1-title", "service-1-desc", "service-2-title", "service-2-desc",
                "service-3-title", "service-3-desc", "service-4-title", "service-4-desc",
                "service-5-title", "service-5-desc", "service-6-title", "service-6-desc",
                "services-cta",
                "why-title", "why-1", "why-2", "why-3", "why-4", "why-5", "why-cta",
                "strategic-label", "strategic-title", "strategic-text",
                "strategic-b1", "strategic-b2", "strategic-b3",
                "about-label", "about-title", "about-text", "about-cta",
                "cta-title", "cta-text", "cta-btn-1", "cta-btn-2",
                "footer-desc", "footer-copyright"));

        PAGE_SECTION_KEYS.put("empresa", List.of(
                "hero-title", "section-title", "section-text",
                "about-label", "about-p2", "about-p3", "about-cta",
                "badge-title", "badge-desc",
                "features-label", "features-title",
                "feature-1-title", "feature-1-desc", "feature-2-title", "feature-2-desc",
                "feature-3-title", "feature-3-desc", "feature-4-title", "feature-4-desc",
                "feature-5-title", "feature-5-desc", "feature-6-title", "feature-6-desc",
                "feature-7-title", "feature-7-desc", "feature-8-title", "feature-8-desc",
                "why-title", "why-1", "why-2", "why-3", "why-4", "why-5", "why-6", "why-cta",
                "mv-label", "mv-title",
                "mision-title", "mision-subtitle", "mision-desc",
                "vision-title", "vision-subtitle", "vision-desc",
                "permits-label", "permits-title", "permits-intro",
                "permit-1-title", "permit-1-desc", "permit-2-title", "permit-2-desc",
                "permit-3-title", "permit-3-desc", "permit-4-title", "permit-4-desc",
                "certs-label", "certs-title",
                "cert-1-title", "cert-1-desc", "cert-2-title", "cert-2-desc",
                "cert-3-title", "cert-3-desc", "cert-4-title", "cert-4-desc",
                "reglamento-text", "reglamento-cta",
                "cta-title", "cta-text", "cta-btn-1", "cta-btn-2",
                "footer-desc", "footer-copyright"));

        PAGE_SECTION_KEYS.put("servicios", List.of(
                "hero-title", "section-title", "section-text",
                "premium-1-cat", "premium-1-title", "premium-1-desc",
                "premium-1-b1", "premium-1-b2", "premium-1-b3", "premium-1-b4", "premium-1-cta",
                "premium-2-cat", "premium-2-title", "premium-2-desc",
                "premium-2-b1", "premium-2-b2", "premium-2-b3", "premium-2-b4", "premium-2-cta",
                "premium-3-cat", "premium-3-title", "premium-3-desc",
                "premium-3-b1", "premium-3-b2", "premium-3-b3", "premium-3-b4", "premium-3-cta",
                "premium-4-cat", "premium-4-title", "premium-4-desc",
                "premium-4-b1", "premium-4-b2", "premium-4-b3", "premium-4-b4", "premium-4-cta",
                "marpol-cat", "marpol-title", "marpol-desc", "marpol-highlight",
                "marpol-b1", "marpol-b2", "marpol-b3", "marpol-b4",
                "cta-title", "cta-text", "cta-btn-1", "cta-btn-2",
                "footer-desc", "footer-copyright"));

        PAGE_SECTION_KEYS.put("procedimientos", List.of(
                "hero-title", "hero-sub",
                "sc-label", "sc-title",
                "card-1-title", "card-1-desc", "card-1-cta",
                "card-2-title", "card-2-desc", "card-2-cta",
                "card-3-title", "card-3-desc", "card-3-cta",
                "reglamento-badge", "reglamento-title", "reglamento-desc", "reglamento-cta",
                "reglamento-card-title", "reglamento-card-desc", "reglamento-card-cta",
                "inscripcion-label", "inscripcion-title",
                "step-1-title", "step-1-desc", "step-2-title", "step-2-desc",
                "step-3-title", "step-3-desc",
                "cta-title", "cta-text", "cta-btn-1", "cta-btn-2",
                "footer-desc", "footer-copyright"));

        PAGE_SECTION_KEYS.put("tramites", List.of(
                "hero-title", "hero-sub", "intro-title", "intro-text",
                "process-title", "process-text",
                "step-1-title", "step-1-desc", "step-2-title", "step-2-desc",
                "step-3-title", "step-3-desc", "step-4-title", "step-4-desc",
                "cat-1-title", "cat-2-title", "cat-3-title", "cat-4-title",
                "cta-title", "cta-text", "cta-btn-1", "cta-btn-2",
                "footer-desc", "footer-copyright"));

        PAGE_SECTION_KEYS.put("galeria", List.of(
                "hero-title", "hero-sub",
                "cta-title", "cta-text", "cta-btn-1", "cta-btn-2",
                "footer-desc", "footer-copyright"));

        PAGE_SECTION_KEYS.put("contacto", List.of(
                "hero-title", "hero-sub", "section-title", "section-text",
                "info-label", "info-title",
                "form-title", "form-desc",
                "lineas-label", "lineas-title", "lineas-text",
                "pqrs-label", "pqrs-title", "pqrs-text",
                "cta-title", "cta-text", "cta-btn-1", "cta-btn-2",
                "footer-desc", "footer-copyright"));

        PAGE_SECTION_KEYS.put("tarifas", List.of(
                "hero-title", "hero-sub",
                "info-title", "info-text", "info-contact-title",
                "legal-note",
                "doc-label", "doc-title", "empty-title", "empty-text",
                "cta-title", "cta-text", "cta-btn-1", "cta-btn-2",
                "footer-desc", "footer-copyright"));

        SECTION_LABELS.put("hero-title", "Título principal (hero)");
        SECTION_LABELS.put("hero-sub", "Subtítulo del hero");
        SECTION_LABELS.put("hero-badge", "Insignia del hero");
        SECTION_LABELS.put("hero-cta-1", "Botón del hero (1)");
        SECTION_LABELS.put("hero-cta-2", "Botón del hero (2)");
        SECTION_LABELS.put("section-title", "Título de sección");
        SECTION_LABELS.put("section-text", "Texto de sección");
        SECTION_LABELS.put("section-label", "Etiqueta de sección");
        SECTION_LABELS.put("intro-title", "Título introductorio");
        SECTION_LABELS.put("intro-text", "Texto introductorio");
        SECTION_LABELS.put("main-title", "Título principal");
        SECTION_LABELS.put("main-text", "Texto principal");
        SECTION_LABELS.put("sc-label", "Etiqueta de sección");
        SECTION_LABELS.put("sc-title", "Título de sección");
        SECTION_LABELS.put("info-label", "Etiqueta de información");
        SECTION_LABELS.put("info-title", "Título de información");
        SECTION_LABELS.put("info-text", "Texto de información");
        SECTION_LABELS.put("info-contact-title", "Título de la caja de contacto");
        SECTION_LABELS.put("legal-note", "Nota legal");
        SECTION_LABELS.put("doc-label", "Etiqueta de documentos");
        SECTION_LABELS.put("doc-title", "Título de documentos");
        SECTION_LABELS.put("empty-title", "Título de estado vacío");
        SECTION_LABELS.put("empty-text", "Texto de estado vacío");
        SECTION_LABELS.put("form-title", "Título del formulario");
        SECTION_LABELS.put("form-desc", "Descripción del formulario");
        SECTION_LABELS.put("lineas-label", "Etiqueta de líneas de atención");
        SECTION_LABELS.put("lineas-title", "Título de líneas de atención");
        SECTION_LABELS.put("lineas-text", "Texto de líneas de atención");
        SECTION_LABELS.put("pqrs-label", "Etiqueta de PQRS");
        SECTION_LABELS.put("pqrs-title", "Título de PQRS");
        SECTION_LABELS.put("pqrs-text", "Texto de PQRS");
        SECTION_LABELS.put("cta-title", "Título de la banda CTA");
        SECTION_LABELS.put("cta-text", "Texto de la banda CTA");
        SECTION_LABELS.put("cta-btn-1", "Botón CTA (1)");
        SECTION_LABELS.put("cta-btn-2", "Botón CTA (2)");
        SECTION_LABELS.put("footer-desc", "Descripción del footer");
        SECTION_LABELS.put("footer-copyright", "Texto de copyright");
        SECTION_LABELS.put("process-title", "Título del proceso");
        SECTION_LABELS.put("process-text", "Texto del proceso");
        SECTION_LABELS.put("services-title", "Título de servicios");
        SECTION_LABELS.put("services-cta", "Botón de servicios");
        SECTION_LABELS.put("why-title", "Título: ¿Por qué escogernos?");
        SECTION_LABELS.put("why-cta", "Botón de ¿Por qué escogernos?");
        SECTION_LABELS.put("strategic-label", "Etiqueta de posición estratégica");
        SECTION_LABELS.put("strategic-title", "Título de posición estratégica");
        SECTION_LABELS.put("strategic-text", "Texto de posición estratégica");
        SECTION_LABELS.put("about-label", "Etiqueta de sobre Mardique");
        SECTION_LABELS.put("about-title", "Título de sobre Mardique");
        SECTION_LABELS.put("about-text", "Texto de sobre Mardique");
        SECTION_LABELS.put("about-cta", "Botón de sobre Mardique");
        SECTION_LABELS.put("badge-title", "Título de la insignia");
        SECTION_LABELS.put("badge-desc", "Descripción de la insignia");
        SECTION_LABELS.put("features-label", "Etiqueta de capacidades");
        SECTION_LABELS.put("features-title", "Título de capacidades");
        SECTION_LABELS.put("mv-label", "Etiqueta de identidad corporativa");
        SECTION_LABELS.put("mv-title", "Título de identidad corporativa");
        SECTION_LABELS.put("permits-label", "Etiqueta de respaldo legal");
        SECTION_LABELS.put("permits-title", "Título de respaldo legal");
        SECTION_LABELS.put("permits-intro", "Texto de respaldo legal");
        SECTION_LABELS.put("certs-label", "Etiqueta de calidad");
        SECTION_LABELS.put("certs-title", "Título de certificaciones");
        SECTION_LABELS.put("reglamento-text", "Texto de reglamento");
        SECTION_LABELS.put("reglamento-cta", "Botón de reglamento");
        SECTION_LABELS.put("reglamento-badge", "Insignia del reglamento");
        SECTION_LABELS.put("reglamento-title", "Título del reglamento");
        SECTION_LABELS.put("reglamento-desc", "Texto del reglamento");
        SECTION_LABELS.put("reglamento-card-title", "Título de la tarjeta del reglamento");
        SECTION_LABELS.put("reglamento-card-desc", "Texto de la tarjeta del reglamento");
        SECTION_LABELS.put("reglamento-card-cta", "Botón de la tarjeta del reglamento");
        SECTION_LABELS.put("inscripcion-label", "Etiqueta de inscripción");
        SECTION_LABELS.put("inscripcion-title", "Título de inscripción");
        SECTION_LABELS.put("marpol-cat", "Categoría MARPOL");
        SECTION_LABELS.put("marpol-title", "Título MARPOL");
        SECTION_LABELS.put("marpol-desc", "Texto MARPOL");
        SECTION_LABELS.put("marpol-highlight", "Destacado MARPOL");
        SECTION_LABELS.put("cat-1-title", "Título de categoría 1");
        SECTION_LABELS.put("cat-2-title", "Título de categoría 2");
        SECTION_LABELS.put("cat-3-title", "Título de categoría 3");
        SECTION_LABELS.put("cat-4-title", "Título de categoría 4");
    }

    // Texto por defecto (original) de cada bloque, por página y clave.
    // IMPORTANTE: estos son valores placeholder razonables. Reemplázalos por el
    // texto REAL que hoy está escrito en cada plantilla pública (Inicio.html,
    // Empresa.html, etc.) para que "Restaurar original" recupere el contenido
    // verdadero y no un texto genérico.
    private static final Map<String, Map<String, String>> DEFAULT_CONTENT = new LinkedHashMap<>();
    static {
        DEFAULT_CONTENT.put("inicio", pageContent(
                "hero-title", "SOCIEDAD PORTUARIA",
                "hero-sub", "Un puerto multipropósito privado de uso público con servicios portuarios y de infraestructura de clase mundial.",
                "hero-badge", "Puerto Multipropósito · Cartagena, Colombia",
                "hero-cta-1", "NUESTROS SERVICIOS",
                "hero-cta-2", "CONÓCENOS",
                "services-title", "NUESTROS SERVICIOS",
                "service-1-title", "GRÚAS MÓVILES",
                "service-1-desc", "Operaciones seguras y eficientes con equipos de última generación.",
                "service-2-title", "HIDROCARBUROS",
                "service-2-desc", "Infraestructura especializada para almacenamiento y manejo de hidrocarburos.",
                "service-3-title", "GRÁNELES",
                "service-3-desc", "Silos y sistemas de manejo de gráneles sólidos con altos estándares.",
                "service-4-title", "CONTENEDORES",
                "service-4-desc", "Servicios integrales para la manipulación y almacenamiento.",
                "service-5-title", "CARGA GENERAL",
                "service-5-desc", "Manejo de carga general y de proyecto con soluciones a la medida.",
                "service-6-title", "SHORE BASE",
                "service-6-desc", "Soporte logístico para la industria offshore y proyectos especiales.",
                "services-cta", "VER TODOS LOS SERVICIOS",
                "why-title", "¿POR QUÉ ESCOGERNOS?",
                "why-1", "Infraestructura moderna y estratégica",
                "why-2", "Talento humano calificado",
                "why-3", "Compromiso con la sostenibilidad",
                "why-4", "Operaciones seguras y eficientes",
                "why-5", "Cercanía y atención personalizada",
                "why-cta", "CONOCE MÁS SOBRE NOSOTROS",
                "strategic-label", "CONECTIVIDAD GLOBAL",
                "strategic-title", "POSICIÓN ESTRATÉGICA",
                "strategic-text", "Nuestra posición estratégica nos convierte en el único puerto con capacidad de disminuir trasbordos y cabotajes en las operaciones de importación y exportación de bienes con conexión directa al interior del país.",
                "strategic-b1", "Conectividad Global Directa",
                "strategic-b2", "Reducción de Tiempos de Tránsito",
                "strategic-b3", "Acceso Multimodal al Interior",
                "about-label", "SOBRE MARDIQUE",
                "about-title", "Comprometidos con el desarrollo del país y la región.",
                "about-text", "Generamos desarrollo y bienestar para la región a través de operaciones responsables, seguras y sostenibles. Más de dos décadas de experiencia nos respaldan.",
                "about-cta", "CONOCE MÁS",
                "cta-title", "¿Listo para operar con nosotros?",
                "cta-text", "Conozca todas las soluciones que Mardique tiene disponibles para su operación.",
                "cta-btn-1", "VER TODOS LOS SERVICIOS",
                "cta-btn-2", "CONTÁCTENOS",
                "footer-desc", "Puerto multipropósito privado de uso público con servicios portuarios y de infraestructura para el desarrollo del país.",
                "footer-copyright", "© 2026 Sociedad Portuaria Mardique S.A. Todos los derechos reservados."));

        DEFAULT_CONTENT.put("empresa", pageContent(
                "hero-title", "SOMOS <strong>MARDIQUE</strong>",
                "section-title", "Un puerto de clase mundial<br>en <span>Colombia</span>",
                "section-text", "Sociedad Portuaria Mardique S.A. es un puerto multipropósito privado de uso público, ubicado estratégicamente en la Carretera Antigua Vía Barú, Corregimiento Santa Ana – Cartagena, Colombia.",
                "about-label", "NUESTRA EMPRESA",
                "about-p2", "Nuestra posición geoestratégica nos permite ser el único puerto con capacidad de disminuir trasbordos y cabotajes en las operaciones de importación y exportación de bienes, con conexión directa al interior del país a través del transporte multimodal fluvial y terrestre.",
                "about-p3", "Contamos con concesión portuaria otorgada por Cormagdalena, y con más de 20 años de experiencia en la prestación de servicios portuarios y logísticos de excelencia.",
                "about-cta", "NUESTROS SERVICIOS",
                "badge-title", "Registro Operador Portuario",
                "badge-desc", "Registrado como operador portuario ante la autoridad competente — Mayo 2021",
                "features-label", "CAPACIDADES",
                "features-title", "Características Portuarias y Logísticas",
                "feature-1-title", "Grúas Móviles",
                "feature-1-desc", "Operaciones seguras y eficientes de izaje con equipos de última generación para todo tipo de carga.",
                "feature-2-title", "Hidrocarburos",
                "feature-2-desc", "Infraestructura especializada para manejo y almacenamiento de crudos y combustibles líquidos derivados del petróleo.",
                "feature-3-title", "Silos para Gráneles",
                "feature-3-desc", "Sistemas de alta capacidad para manejo de gráneles sólidos con altos estándares de calidad y seguridad.",
                "feature-4-title", "Contenedores",
                "feature-4-desc", "Manipulación integral y almacenamiento de contenedores con infraestructura especializada.",
                "feature-5-title", "Carga General y de Proyecto",
                "feature-5-desc", "Manejo integral de carga general con soluciones logísticas a la medida del cliente.",
                "feature-6-title", "Shore Base",
                "feature-6-desc", "Soporte logístico especializado para la industria offshore y proyectos de gran envergadura.",
                "feature-7-title", "Transporte Terrestre",
                "feature-7-desc", "Operaciones de transporte terrestre integradas con la cadena logística portuaria.",
                "feature-8-title", "Transporte Fluvial",
                "feature-8-desc", "Operaciones fluviales con conexión directa al interior del país a través del río Magdalena.",
                "why-title", "¿POR QUÉ ESCOGERNOS?",
                "why-1", "Localización Geoestratégica",
                "why-2", "Infraestructura y extensión de área",
                "why-3", "Clima de inversión y regulación",
                "why-4", "Conexión a centros logísticos nacionales",
                "why-5", "Terminal marítimo y fluvial simultáneo",
                "why-6", "Trámites en línea disponibles",
                "why-cta", "VER SERVICIOS",
                "mv-label", "IDENTIDAD CORPORATIVA",
                "mv-title", "Nuestra <span>Razón de Ser</span>",
                "mision-title", "Misión",
                "mision-subtitle", "Lo que hacemos hoy",
                "mision-desc", "Prestar servicios portuarios y logísticos con excelencia, con mayores y mejores ventajas competitivas para nuestros usuarios y clientes; ofreciéndoles procesos eficientes, creados a la medida de sus necesidades.",
                "vision-title", "Visión",
                "vision-subtitle", "Hacia dónde vamos",
                "vision-desc", "Ser la Terminal más importante en Colombia en prestación de servicios portuarios y logísticos, a través de una gran plataforma logística integrada por el transporte multimodal, que impulse el desarrollo de las diferentes regiones y actores del comercio exterior.",
                "permits-label", "RESPALDO LEGAL",
                "permits-title", "Cuenta con Permisos Oficiales",
                "permits-intro", "Mardique opera bajo el más riguroso marco legal y regulatorio, con autorizaciones y permisos vigentes de las entidades competentes.",
                "permit-1-title", "Concesión Portuaria",
                "permit-1-desc", "Otorgada por Cormagdalena para la operación de las facilidades fluviales y marítimas.",
                "permit-2-title", "Licencia Ambiental",
                "permit-2-desc", "Otorgada por Cardique, garantizando estándares ambientales en todas las operaciones.",
                "permit-3-title", "Autorización Ministerio de Minas",
                "permit-3-desc", "Para almacenamiento de crudos y combustibles líquidos derivados del petróleo.",
                "permit-4-title", "Zona Franca Portuaria Especial — DIAN",
                "permit-4-desc", "Resolución 002639 — Abril de 2014. Beneficios tributarios y aduaneros para operaciones de comercio exterior.",
                "certs-label", "CALIDAD",
                "certs-title", "Certificaciones Vigentes",
                "cert-1-title", "Gestión de Calidad",
                "cert-1-desc", "Sistema de gestión de calidad certificado por Bureau Veritas — ISO 9001:2015",
                "cert-2-title", "Seguridad y Salud",
                "cert-2-desc", "Gestión de seguridad y salud en el trabajo certificado internacionalmente — ISO 45001:2018",
                "cert-3-title", "Operador Portuario",
                "cert-3-desc", "Registro oficial como operador portuario — Mayo 2021",
                "cert-4-title", "Zona Franca",
                "cert-4-desc", "Portuaria especial otorgada por la DIAN desde 2014",
                "reglamento-text", "Reglamento técnico de operaciones",
                "reglamento-cta", "VER REGLAMENTO",
                "cta-title", "¿Listo para trabajar con nosotros?",
                "cta-text", "Conozca todos los servicios que Mardique tiene disponibles para su operación.",
                "cta-btn-1", "CONOZCA NUESTROS SERVICIOS",
                "cta-btn-2", "CONTÁCTENOS",
                "footer-desc", "Puerto multipropósito privado de uso público. Servicios portuarios y logísticos de excelencia en Cartagena, Colombia.",
                "footer-copyright", "© 2026 Sociedad Portuaria Mardique S.A. Todos los derechos reservados."));

        DEFAULT_CONTENT.put("servicios", pageContent(
                "hero-title", "Nuestros <strong>Servicios</strong>",
                "section-title", "Servicios Marítimos y Fluviales",
                "section-text", "Operaciones de muellaje y atraque de embarcaciones respaldadas por infraestructura robusta y tecnología de precisión para garantizar la eficiencia en cada maniobra.",
                "premium-1-cat", "Operaciones Mayores",
                "premium-1-title", "Muelle Marítimo",
                "premium-1-desc", "Operaciones de muellaje y atraque de embarcaciones respaldadas por infraestructura robusta y tecnología de precisión para garantizar la eficiencia en cada maniobra.",
                "premium-1-b1", "Muelle marítimo de 150 Mts de longitud operativa.",
                "premium-1-b2", "Calado natural excepcional de 15 Mts.",
                "premium-1-b3", "Capacidad para buques de 180 a 200 mts de eslora.",
                "premium-1-b4", "Viaducto con 3 carriles y piñas de amarre externas.",
                "premium-1-cta", "Solicitar Cotización",
                "premium-2-cat", "Conexión Interior",
                "premium-2-title", "Muelle Fluvial",
                "premium-2-desc", "Nuestra posición privilegiada permite operaciones de muellaje y atraque simultáneo de convoy, remolcadores, y embarcaciones marítimas y fluviales, conectando sus cargas con el interior del país.",
                "premium-2-b1", "Línea de atraque especializada de 140 Mts.",
                "premium-2-b2", "Plataforma principal operativa de 1.400 Mts².",
                "premium-2-b3", "Profundidad garantizada de 12 pies.",
                "premium-2-b4", "Ventaja competitiva para distribución nacional.",
                "premium-2-cta", "Solicitar Cotización",
                "premium-3-cat", "Carga y Descarga",
                "premium-3-title", "Servicios Portuarios",
                "premium-3-desc", "Contamos con personal altamente calificado, equipos de última tecnología e infraestructura dispuesta a ofrecerle el mejor manejo para su carga, diseñando servicios a la medida de sus necesidades logísticas.",
                "premium-3-b1", "Cargue, descargue y recibo de mercancía.",
                "premium-3-b2", "Almacenamiento, acopio y conservación segura.",
                "premium-3-b3", "Embalaje, empaque y reempaque especializado.",
                "premium-3-b4", "Control de inventarios en tiempo real.",
                "premium-3-cta", "Más información",
                "premium-4-cat", "Distribución Global",
                "premium-4-title", "Plataforma Logística",
                "premium-4-desc", "Somos una gran plataforma portuaria que brinda a sus clientes y usuarios la integración de todos los servicios en un solo lugar. Apoyamos el montaje de centros de distribución internacional.",
                "premium-4-b1", "Distribución desde Colombia hacia el exterior.",
                "premium-4-b2", "Distribución ágil a nivel nacional.",
                "premium-4-b3", "Integración total de la cadena de suministro.",
                "premium-4-b4", "Recibo eficiente de productos de exportación.",
                "premium-4-cta", "Más información",
                "marpol-cat", "Gestión Ambiental",
                "marpol-title", "Planta <span>MARPOL</span>",
                "marpol-desc", "Somos el único Terminal en Colombia que cuenta con una planta especializada para el tratamiento y disposición de residuos provenientes de buques, garantizando el estricto cumplimiento de la normativa ambiental internacional.",
                "marpol-highlight", "Capacidad de 65.000 Toneladas Anuales",
                "marpol-b1", "Tratamiento Marpol Anexo I — Hidrocarburos",
                "marpol-b2", "Tratamiento Marpol Anexo IV — Aguas Residuales",
                "marpol-b3", "Tratamiento Marpol Anexo V — Basuras",
                "marpol-b4", "Tratamiento Marpol Anexo VI — Contaminación Atmosférica",
                "cta-title", "¿Listo para operar con nosotros?",
                "cta-text", "Conozca todas las soluciones que Mardique tiene disponibles para su operación.",
                "cta-btn-1", "Ver todos los servicios",
                "cta-btn-2", "Contáctenos",
                "footer-desc", "Puerto multipropósito privado de uso público con servicios portuarios y de infraestructura para el desarrollo del país.",
                "footer-copyright", "© 2026 Sociedad Portuaria Mardique S.A. Todos los derechos reservados."));

        DEFAULT_CONTENT.put("procedimientos", pageContent(
                "hero-title", "Servicio al <strong>Cliente</strong>",
                "hero-sub", "Conozca los trámites, procedimientos e inscripciones disponibles para clientes y proveedores de Mardique.",
                "sc-label", "Servicio al Cliente",
                "sc-title", "¿Cómo podemos ayudarle?",
                "card-1-title", "Trámites en Línea",
                "card-1-desc", "Facilitamos sus solicitudes de servicio con formatos digitales descargables para programar sus operaciones de manera oportuna.",
                "card-1-cta", "Ir a Trámites",
                "card-2-title", "Procedimientos",
                "card-2-desc", "Guías y orientación para realizar sus solicitudes de servicio, incluyendo el reglamento técnico de operaciones vigente.",
                "card-2-cta", "Ver Procedimientos",
                "card-3-title", "Inscripción",
                "card-3-desc", "Realice su proceso de registro oficial como cliente o proveedor para iniciar operaciones con nosotros de forma segura.",
                "card-3-cta", "Inscribirse",
                "reglamento-badge", "Documento oficial",
                "reglamento-title", "Reglamento Técnico de <span>Operaciones</span>",
                "reglamento-desc", "Para realizar sus solicitudes de servicio, tendrá a su disposición la guía y orientación que encontrará en el Reglamento de Condiciones Técnicas de Operación para las Facilidades Fluviales y Marítimas de Mardique.",
                "reglamento-cta", "Descargar Reglamento PDF",
                "reglamento-card-title", "¿Qué contiene el Reglamento de Operaciones?",
                "reglamento-card-desc", "Este documento establece las condiciones técnicas y operativas bajo las cuales se prestan los servicios en las instalaciones fluviales y marítimas de la Sociedad Portuaria Mardique. Incluye normas de seguridad, procedimientos de ingreso, manejo de cargas y responsabilidades de los usuarios.",
                "reglamento-card-cta", "Ver documento completo",
                "inscripcion-label", "Registro de Usuarios",
                "inscripcion-title", "Inscripción de Clientes y Proveedores",
                "step-1-title", "Diligencie el formulario SIPLAFT",
                "step-1-desc", "Descargue y complete el formulario oficial del Sistema de Prevención de Lavado de Activos y Financiación del Terrorismo (SIPLAFT) con toda la información de su empresa.",
                "step-2-title", "Envíe el formulario al correo electrónico",
                "step-2-desc", "Una vez diligenciado, envíe el formato al correo <strong>proteccion@spmardique.com</strong> indicando en el asunto el tipo de inscripción.",
                "step-3-title", "Espere la confirmación",
                "step-3-desc", "Nuestro equipo revisará su solicitud y le notificará sobre la aprobación del registro para que pueda iniciar operaciones con Mardique.",
                "cta-title", "¿Listo para operar con Mardique?",
                "cta-text", "Descargue los formatos y comience su proceso de registro o solicitud de servicios hoy mismo.",
                "cta-btn-1", "Ir a Trámites en Línea",
                "cta-btn-2", "Contáctenos",
                "footer-desc", "Puerto multipropósito privado de uso público con servicios portuarios y de infraestructura para el desarrollo del país.",
                "footer-copyright", "© 2026 Sociedad Portuaria Mardique S.A. Todos los derechos reservados."));

        DEFAULT_CONTENT.put("tramites", pageContent(
                "hero-title", "Trámites <strong>en Línea</strong>",
                "hero-sub", "Descargue y diligencie los formatos requeridos para iniciar sus operaciones con nosotros de manera ágil y segura.",
                "intro-title", "Formatos digitales para su gestión portuaria",
                "intro-text", "Con los trámites en línea, facilitamos la manera de realizar solicitudes de servicio, brindando respuesta oportuna y realizando la programación de los mismos. Descargue los formatos según su perfil.",
                "process-title", "¿Cómo usar estos formatos?",
                "process-text", "Descargue el formato correspondiente a su tipo de operación, diligencie todos los campos requeridos y envíelo al correo electrónico indicado en cada formulario. Nuestro equipo le responderá para programar y confirmar su servicio.",
                "step-1-title", "Descargue el formato",
                "step-1-desc", "Identifique el formulario según su categoría de operación.",
                "step-2-title", "Diligencie el documento",
                "step-2-desc", "Complete todos los campos requeridos con información precisa.",
                "step-3-title", "Envíe al correo indicado",
                "step-3-desc", "Remita el formato al correo especificado en cada sección.",
                "step-4-title", "Reciba confirmación",
                "step-4-desc", "Nuestro equipo programará y confirmará su operación.",
                "cat-1-title", "Inscripción de Clientes y Proveedores",
                "cat-2-title", "Importadores, Exportadores y Agencias de Aduana",
                "cat-3-title", "Agencia Marítima",
                "cat-4-title", "Autorización de Ingreso y Salida de Personal, Materiales y Vehículos",
                "cta-title", "¿Necesita orientación con los trámites?",
                "cta-text", "Nuestro equipo de servicio al cliente está disponible para guiarle en cada paso del proceso.",
                "cta-btn-1", "Ver Procedimientos",
                "cta-btn-2", "Contáctenos",
                "footer-desc", "Puerto multipropósito privado de uso público con servicios portuarios y de infraestructura para el desarrollo del país.",
                "footer-copyright", "© 2026 Sociedad Portuaria Mardique S.A. Todos los derechos reservados."));

        DEFAULT_CONTENT.put("galeria", pageContent(
                "hero-title", "Nuestras <strong>Instalaciones</strong>",
                "hero-sub", "Conozca las instalaciones, equipos y operaciones del puerto multipropósito de Mardique en el Caribe colombiano.",
                "cta-title", "¿Le interesa operar con nosotros?",
                "cta-text", "Contáctenos y descubra cómo Mardique puede ser su aliado portuario estratégico en el Caribe colombiano.",
                "cta-btn-1", "Contáctenos",
                "cta-btn-2", "Ver Servicios",
                "footer-desc", "Puerto privado multipropósito en el Caribe colombiano. Comprometidos con la excelencia operativa.",
                "footer-copyright", "© 2026 Sociedad Portuaria Mardique S.A. Todos los derechos reservados."));

        DEFAULT_CONTENT.put("contacto", pageContent(
                "hero-title", "Canales de <strong>Comunicación</strong>",
                "hero-sub", "Estamos listos para atenderle. Contáctenos a través de cualquiera de nuestros canales y un representante le responderá a la brevedad.",
                "section-title", "Estamos aquí<br>para <strong style=\"color:var(--accent-gold)\">ayudarle</strong>",
                "section-text", "En Sociedad Portuaria Mardique nos comprometemos a dar respuesta oportuna a todas sus solicitudes. Nuestro equipo se encuentra disponible para orientarle en sus operaciones portuarias, comerciales y logísticas.",
                "info-label", "Comuníquese con nosotros",
                "info-title", "Estamos aquí para ayudarle",
                "form-title", "Envíenos un Mensaje",
                "form-desc", "Complete el formulario y nuestro equipo se pondrá en contacto con usted a la brevedad posible.",
                "lineas-label", "Directorio de Contactos",
                "lineas-title", "Líneas de <strong style=\"color:var(--accent-gold)\">Atención</strong>",
                "lineas-text", "Ponemos a su disposición nuestros canales directos. Cada área cuenta con un representante especializado para atender sus requerimientos.",
                "pqrs-label", "Canal Formal",
                "pqrs-title", "Quejas, Peticiones<br>y <strong style=\"color:var(--accent-gold)\">Reclamos</strong>",
                "pqrs-text", "Si no encontró respuesta a su inquietud, o tiene alguna queja o reclamo, puede diligenciar el formulario. Atendemos todas las solicitudes con un plazo máximo de respuesta establecido por ley.",
                "cta-title", "¿Listo para operar con nosotros?",
                "cta-text", "Únase a los clientes que confían en Mardique para sus operaciones portuarias y logísticas en el Caribe colombiano.",
                "cta-btn-1", "Ver nuestros servicios",
                "cta-btn-2", "Reglamento de Operaciones",
                "footer-desc", "Puerto privado multipropósito en el Caribe colombiano. Comprometidos con la excelencia operativa y la satisfacción de nuestros clientes.",
                "footer-copyright", "© 2026 Sociedad Portuaria Mardique S.A. Todos los derechos reservados."));

        DEFAULT_CONTENT.put("tarifas", pageContent(
                "hero-title", "Nuestras <strong>Tarifas</strong>",
                "hero-sub", "Consulte las tarifas de nuestros servicios portuarios, marítimos y logísticos. Para información detallada, nuestro equipo comercial está disponible para atenderle.",
                "info-title", "Información de <span>Tarifas</span>",
                "info-text", "Para conocer las tarifas actualizadas de nuestros servicios portuarios, marítimos, fluviales y logísticos, por favor comuníquese directamente con nuestro equipo comercial. Estaremos encantados de entregarle una cotización personalizada según su tipo de operación y volumen de carga.",
                "info-contact-title", "Contáctenos para obtener sus tarifas",
                "legal-note", "Las tarifas de la Sociedad Portuaria Mardique están sujetas a los lineamientos de la Superintendencia de Puertos y Transporte de Colombia.",
                "doc-label", "Documento de Tarifas",
                "doc-title", "Consulte nuestras <span style=\"color:var(--accent-gold)\">tarifas</span>",
                "empty-title", "Documento de tarifas no disponible",
                "empty-text", "El documento de tarifas aún no ha sido cargado por nuestro equipo. Por favor, contacte a nuestro equipo comercial para obtener información actualizada.",
                "cta-title", "¿Listo para comenzar su operación?",
                "cta-text", "Contáctenos y obtenga una cotización personalizada adaptada a sus necesidades.",
                "cta-btn-1", "Solicitar Cotización",
                "cta-btn-2", "Ver Trámites en Línea",
                "footer-desc", "Puerto multipropósito privado de uso público con servicios portuarios y de infraestructura para el desarrollo del país.",
                "footer-copyright", "© 2026 Sociedad Portuaria Mardique S.A. Todos los derechos reservados."));
    }

    private static Map<String, String> pageContent(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private String defaultContentFor(String page, String sectionKey) {
        return DEFAULT_CONTENT.getOrDefault(page, Map.of())
                .getOrDefault(sectionKey, "Contenido predeterminado");
    }

    private String pageLabel(String page) {
        return switch (page) {
            case "tramites" -> "Trámites en Línea";
            case "galeria" -> "Galería";
            case "contacto" -> "Contacto";
            default -> page.substring(0, 1).toUpperCase() + page.substring(1);
        };
    }

    /**
     * Devuelve TODOS los bloques de contenido de una página, creando en Mongo
     * los que falten. A diferencia de la versión anterior, esto NO se detiene
     * en cuanto la página tiene algún registro: sincroniza contra
     * PAGE_SECTION_KEYS en cada llamada, así que si agregas una clave nueva a
     * ese mapa, aparecerá automáticamente en el editor sin tener que borrar
     * nada en la base de datos. Esta es la causa de que antes solo aparecieran
     * "pocas secciones": una vez creado el primer registro, las claves nuevas
     * jamás se generaban.
     */
    private List<PageContent> ensurePageContent(String page) {
        List<PageContent> existing = pageContentRepository.findByPage(page);
        Map<String, PageContent> byKey = existing.stream()
                .collect(Collectors.toMap(PageContent::getSectionKey, pc -> pc, (a, b) -> a));

        List<String> keys = PAGE_SECTION_KEYS.getOrDefault(page, List.of("hero-title", "hero-sub"));
        List<PageContent> result = new ArrayList<>();
        for (String key : keys) {
            PageContent pc = byKey.get(key);
            if (pc == null) {
                String def = defaultContentFor(page, key);
                pc = new PageContent(page, key, def, SECTION_LABELS.getOrDefault(key, key));
                pc.setOriginalContent(def);
                pageContentRepository.save(pc);
            } else if (pc.getOriginalContent() == null || pc.getOriginalContent().isEmpty()) {
                // Registro creado antes de este arreglo: se le rellena el
                // originalContent para que "Restaurar original" funcione.
                pc.setOriginalContent(defaultContentFor(page, key));
                pageContentRepository.save(pc);
            }
            result.add(pc);
        }
        return result;
    }

    private boolean allowedFile(String filename, Set<String> exts) {
        if (filename == null || filename.isEmpty()) return false;
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
        return exts.contains(ext);
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        model.addAttribute("username", auth.getName());
        model.addAttribute("contactCount", contactRepository.count());
        model.addAttribute("documentCount", documentRepository.count());
        model.addAttribute("galleryCount", galleryImageRepository.count());
        model.addAttribute("contentPages", List.of("contacto", "tramites", "procedimientos", "tarifas", "galeria"));
        return "EditorDashboard";
    }

    @GetMapping("/contacts")
    public String listContacts(Model model) {
        model.addAttribute("contacts", contactRepository.findAll());
        model.addAttribute("contact", new Contact());
        return "EditorContacts";
    }

    @PostMapping("/contacts/save")
    public String saveContact(@ModelAttribute Contact contact, RedirectAttributes ra) {
        if (contact.getId() != null) {
            contactRepository.findById(contact.getId()).ifPresent(existing -> {
                contact.setCreatedAt(existing.getCreatedAt());
            });
        }
        contactRepository.save(contact);
        ra.addFlashAttribute("success", "Contacto guardado correctamente");
        return "redirect:/editor/contacts";
    }

    @GetMapping("/contacts/edit/{id}")
    public String editContact(@PathVariable Long id, Model model) {
        model.addAttribute("contacts", contactRepository.findAll());
        model.addAttribute("contact", contactRepository.findById(id).orElseThrow(() -> new RuntimeException("Contact not found: " + id)));
        return "EditorContacts";
    }

    @PostMapping("/contacts/delete/{id}")
    public String deleteContact(@PathVariable Long id, RedirectAttributes ra) {
        contactRepository.deleteById(id);
        ra.addFlashAttribute("success", "Contacto eliminado");
        return "redirect:/editor/contacts";
    }

    @GetMapping("/documents")
    public String listDocuments(Model model) {
        Map<String, Document> tramiteCardDocs = documentRepository.findByTipoAndCardKeyIsNotNull("TRAMITE")
                .stream().filter(d -> d.getCardKey() != null).collect(Collectors.toMap(Document::getCardKey, d -> d, (a, b) -> a));
        model.addAttribute("tramiteCardDocs", tramiteCardDocs);
        model.addAttribute("procedimientos", documentRepository.findByTipo("PROCEDIMIENTO"));
        model.addAttribute("tarifas", documentRepository.findByTipo("TARIFA"));
        model.addAttribute("document", new Document());
        model.addAttribute("tramiteCards", List.of(
                Map.of("key", "INSCRIPCION_CLIENTES", "label", "Inscripci\u00f3n de Clientes", "roles", "CLIENTE,PROVEEDOR"),
                Map.of("key", "DECLARACION_LAFT", "label", "Declaraci\u00f3n Prevenci\u00f3n LAFT", "roles", "CLIENTE,PROVEEDOR,EMPRESA"),
                Map.of("key", "CERTIFICACION_LAFT", "label", "Certificaci\u00f3n Prevenci\u00f3n LAFT", "roles", "CLIENTE,PROVEEDOR,EMPRESA"),
                Map.of("key", "ACUERDO_SEGURIDAD", "label", "Acuerdo de Seguridad", "roles", "CLIENTE,PROVEEDOR,EMPRESA"),
                Map.of("key", "SOLICITUDES_PROGRAMABLES", "label", "Solicitudes Programables", "roles", "CLIENTE,PROVEEDOR"),
                Map.of("key", "SOLICITUD_EMBARQUE", "label", "Solicitud de Embarque", "roles", "CLIENTE,PROVEEDOR"),
                Map.of("key", "BOOKING", "label", "Formato Booking", "roles", "CLIENTE,PROVEEDOR"),
                Map.of("key", "AUTORIZACION_INGRESO", "label", "Autorizaci\u00f3n Ingreso/Salida", "roles", "CLIENTE,PROVEEDOR,EMPRESA"),
                Map.of("key", "REGLAMENTO", "label", "Reglamento Operativo", "roles", "")
        ));
        model.addAttribute("allRoles", List.of("CLIENTE", "PROVEEDOR", "EMPRESA", "PERSONA"));
        return "EditorDocuments";
    }

    @PostMapping("/documents/upload")
    public String uploadDocument(@ModelAttribute Document document,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String email,
                                 @RequestParam(required = false) String emailCc,
                                 @RequestParam(required = false) String descripcion,
                                 @RequestParam(required = false) String destinatarios,
                                 RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Debe seleccionar un archivo");
            return "redirect:/editor/documents";
        }
        if (!allowedFile(file.getOriginalFilename(), ALLOWED_EXTENSIONS)) {
            ra.addFlashAttribute("error", "Tipo de archivo no permitido");
            return "redirect:/editor/documents";
        }
        try {
            String url = cloudinaryService.uploadFile(file);
            document.setFileName(file.getOriginalFilename());
            document.setFilePath(url);
            document.setUploadedAt(LocalDate.now());
            document.setEmail(email);
            document.setEmailCc(emailCc);
            document.setDescripcion(descripcion);
            document.setDestinatarios(destinatarios);
            documentRepository.save(document);
            ra.addFlashAttribute("success", "Documento subido correctamente");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Error al subir el archivo: " + e.getMessage());
        }
        return "redirect:/editor/documents";
    }

    @PostMapping("/documents/meta")
    public String updateDocumentMeta(@RequestParam Long id, @RequestParam(required = false) String email,
                                     @RequestParam(required = false) String emailCc,
                                     @RequestParam(required = false) String descripcion,
                                     @RequestParam(required = false) String nombre,
                                     @RequestParam(required = false) String destinatarios,
                                     RedirectAttributes ra) {
        Document doc = documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found: " + id));
        if (email != null) doc.setEmail(email);
        if (emailCc != null) doc.setEmailCc(emailCc);
        if (descripcion != null) doc.setDescripcion(descripcion);
        if (nombre != null) doc.setNombre(nombre);
        if (destinatarios != null) doc.setDestinatarios(destinatarios);
        documentRepository.save(doc);
        ra.addFlashAttribute("success", "Metadatos actualizados correctamente");
        return "redirect:/editor/documents";
    }

    @PostMapping("/documents/delete/{id}")
    public String deleteDocument(@PathVariable Long id, RedirectAttributes ra) {
        Document doc = documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found: " + id));
        documentRepository.deleteById(id);
        ra.addFlashAttribute("success", "Documento eliminado");
        return "redirect:/editor/documents";
    }


    @GetMapping("/gallery")
    public String listGallery(Model model) {
        model.addAttribute("images", galleryImageRepository.findAll());
        model.addAttribute("image", new GalleryImage());
        return "EditorGallery";
    }

    @PostMapping("/gallery/upload")
    public String uploadImage(@ModelAttribute GalleryImage image,
                              @RequestParam("file") MultipartFile file,
                              RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Debe seleccionar una imagen");
            return "redirect:/editor/gallery";
        }
        if (!allowedFile(file.getOriginalFilename(), ALLOWED_IMAGE_EXTENSIONS)) {
            ra.addFlashAttribute("error", "Formato de imagen no permitido (png, jpg, gif, webp, svg)");
            return "redirect:/editor/gallery";
        }
        try {
            String url = cloudinaryService.uploadFile(file);
            image.setFileName(file.getOriginalFilename());
            image.setFilePath(url);
            image.setUploadedAt(LocalDate.now());
            image.setActive(true);
            galleryImageRepository.save(image);
            ra.addFlashAttribute("success", "Imagen subida correctamente");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Error al subir imagen: " + e.getMessage());
        }
        return "redirect:/editor/gallery";
    }

    @PostMapping("/gallery/toggle/{id}")
    public String toggleImage(@PathVariable Long id, RedirectAttributes ra) {
        GalleryImage img = galleryImageRepository.findById(id).orElseThrow(() -> new RuntimeException("GalleryImage not found: " + id));
        img.setActive(!img.isActive());
        galleryImageRepository.save(img);
        ra.addFlashAttribute("success", img.isActive() ? "Imagen activada" : "Imagen desactivada");
        return "redirect:/editor/gallery";
    }

    @PostMapping("/gallery/delete/{id}")
    public String deleteImage(@PathVariable Long id, RedirectAttributes ra) {
        galleryImageRepository.deleteById(id);
        ra.addFlashAttribute("success", "Imagen eliminada");
        return "redirect:/editor/gallery";
    }


    @GetMapping("/content")
    public String listContentPages(Model model) {
        model.addAttribute("pages", CONTENT_PAGES);
        return "EditorContent";
    }

    @GetMapping("/content/{page}")
    public String editPageContent(@PathVariable String page, Model model) {
        if (page == null || page.isEmpty() || !CONTENT_PAGES.contains(page)) return "redirect:/editor/content";
        List<PageContent> contents = ensurePageContent(page);
        model.addAttribute("page", page);
        model.addAttribute("contents", contents);
        model.addAttribute("pageLabel", pageLabel(page));
        model.addAttribute("sectionLabels", SECTION_LABELS);
        model.addAttribute("pages", CONTENT_PAGES);
        return "EditorContent";
    }

    @PostMapping("/content/save")
    @ResponseBody
    public String savePageContent(@RequestParam String sectionKey, @RequestParam String content,
                                  @RequestParam String page) {
        if (!CONTENT_PAGES.contains(page)) return "NOT_FOUND";

        String sanitized = Jsoup.clean(content, Safelist.basic()
                .addTags("strong", "em", "u", "br", "p", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li")
                .addAttributes("a", "href", "title")
        );

        PageContent pc = pageContentRepository.findByPageAndSectionKey(page, sectionKey).orElse(null);
        if (pc == null) {
            // No basta con que la clave no exista todavía en Mongo: si es una
            // clave declarada en PAGE_SECTION_KEYS la creamos aquí mismo, en
            // vez de devolver NOT_FOUND y perder silenciosamente el cambio.
            if (!PAGE_SECTION_KEYS.getOrDefault(page, List.of()).contains(sectionKey)) {
                return "NOT_FOUND";
            }
            pc = new PageContent(page, sectionKey, sanitized, SECTION_LABELS.getOrDefault(sectionKey, sectionKey));
            pc.setOriginalContent(defaultContentFor(page, sectionKey));
        } else {
            pc.setContent(sanitized);
        }
        pageContentRepository.save(pc);
        return "OK";
    }

    @GetMapping("/content/restore/{sectionKey}")
    @ResponseBody
    public Map<String, String> restoreContent(@PathVariable String sectionKey, @RequestParam String page) {
        PageContent pc = pageContentRepository.findByPageAndSectionKey(page, sectionKey).orElse(null);
        String original = (pc != null && pc.getOriginalContent() != null && !pc.getOriginalContent().isEmpty())
                ? pc.getOriginalContent()
                : defaultContentFor(page, sectionKey);

        if (pc != null) {
            pc.setContent(original);
            pageContentRepository.save(pc);
        }
        return Map.of("content", original);
    }


    @GetMapping("/config")
    public String listConfig(Model model) {
        model.addAttribute("iframeUrl", systemConfigRepository.findByConfigKey("TARIFA_IFRAME_URL").orElse(null));
        model.addAttribute("tarifaPhone", systemConfigRepository.findByConfigKey("TARIFA_PHONE").orElse(null));
        model.addAttribute("tarifaEmail", systemConfigRepository.findByConfigKey("TARIFA_EMAIL").orElse(null));
        return "EditorConfig";
    }

    @PostMapping("/config/update")
    public String updateConfig(@RequestParam String key, @RequestParam String value, RedirectAttributes ra) {
        SystemConfig config = systemConfigRepository.findByConfigKey(key)
                .orElseGet(() -> {
                    SystemConfig sc = new SystemConfig();
                    sc.setConfigKey(key);
                    return sc;
                });
        config.setConfigValue(value);
        systemConfigRepository.save(config);
        ra.addFlashAttribute("success", "Configuraci\u00f3n actualizada correctamente");
        return "redirect:/editor/config";
    }
}