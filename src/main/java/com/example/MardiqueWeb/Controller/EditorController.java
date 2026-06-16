package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.*;
import com.example.MardiqueWeb.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    private final Path uploadDir = Paths.get("uploads");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "gif", "webp", "svg", "doc", "docx", "xls", "xlsx");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "svg");

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
            Map.of("key", "INSCRIPCION_CLIENTES", "label", "Inscripci\u00f3n de Clientes"),
            Map.of("key", "DECLARACION_LAFT", "label", "Declaraci\u00f3n Prevenci\u00f3n LAFT"),
            Map.of("key", "CERTIFICACION_LAFT", "label", "Certificaci\u00f3n Prevenci\u00f3n LAFT"),
            Map.of("key", "ACUERDO_SEGURIDAD", "label", "Acuerdo de Seguridad"),
            Map.of("key", "SOLICITUDES_PROGRAMABLES", "label", "Solicitudes Programables"),
            Map.of("key", "SOLICITUD_EMBARQUE", "label", "Solicitud de Embarque"),
            Map.of("key", "BOOKING", "label", "Formato Booking"),
            Map.of("key", "AUTORIZACION_INGRESO", "label", "Autorizaci\u00f3n Ingreso/Salida"),
            Map.of("key", "REGLAMENTO", "label", "Reglamento Operativo")
        ));
        return "EditorDocuments";
    }

    @PostMapping("/documents/upload")
    public String uploadDocument(@ModelAttribute Document document,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) String email,
                                 @RequestParam(required = false) String emailCc,
                                 @RequestParam(required = false) String descripcion,
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
            Files.createDirectories(uploadDir.resolve(document.getTipo().toLowerCase()));
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path targetPath = uploadDir.resolve(document.getTipo().toLowerCase()).resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            document.setFileName(file.getOriginalFilename());
            document.setFilePath("/uploads/" + document.getTipo().toLowerCase() + "/" + fileName);
            document.setUploadedAt(LocalDate.now());
            document.setEmail(email);
            document.setEmailCc(emailCc);
            document.setDescripcion(descripcion);
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
                                      RedirectAttributes ra) {
        Document doc = documentRepository.findById(id).orElseThrow(() -> new RuntimeException("Document not found: " + id));
        if (email != null) doc.setEmail(email);
        if (emailCc != null) doc.setEmailCc(emailCc);
        if (descripcion != null) doc.setDescripcion(descripcion);
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

    // ============== GALERIA ==============

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
            Files.createDirectories(uploadDir.resolve("gallery"));
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path targetPath = uploadDir.resolve("gallery").resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            image.setFileName(file.getOriginalFilename());
            image.setFilePath("/uploads/gallery/" + fileName);
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

    // ============== CONTENIDO DE PAGINAS ==============

    @GetMapping("/content")
    public String listContentPages(Model model) {
        List<String> pages = List.of("inicio", "empresa", "servicios", "procedimientos", "contacto", "galeria", "tramitesenlinea", "tarifas");
        model.addAttribute("pages", pages);
        return "EditorContent";
    }

    @GetMapping("/content/{page}")
    public String editPageContent(@PathVariable String page, Model model) {
        if (page == null || page.isEmpty()) return "redirect:/editor/content";
        List<PageContent> contents = pageContentRepository.findByPage(page);
        model.addAttribute("page", page);
        model.addAttribute("contents", contents);
        model.addAttribute("pageLabel", page.substring(0, 1).toUpperCase() + page.substring(1));
        model.addAttribute("pages", List.of("inicio", "empresa", "servicios", "procedimientos", "contacto", "galeria", "tramitesenlinea", "tarifas"));
        return "EditorContent";
    }

    @PostMapping("/content/save")
    @ResponseBody
    public String savePageContent(@RequestParam String sectionKey, @RequestParam String content,
                                   @RequestParam String page) {
        PageContent pc = pageContentRepository.findByPageAndSectionKey(page, sectionKey).orElse(null);
        if (pc != null) {
            pc.setContent(content);
            pageContentRepository.save(pc);
            return "OK";
        }
        return "NOT_FOUND";
    }

    @GetMapping("/content/restore/{sectionKey}")
    @ResponseBody
    public Map<String, String> restoreContent(@PathVariable String sectionKey, @RequestParam String page) {
        PageContent pc = pageContentRepository.findByPageAndSectionKey(page, sectionKey).orElse(null);
        String defaultContent = switch (sectionKey) {
            case "hero-title" -> "Mardique";
            case "hero-subtitle" -> "Soluciones portuarias";
            case "section1-title" -> "Servicios";
            case "section1-text" -> "Contenido de la sección";
            case "section2-title" -> "Contacto";
            case "section2-text" -> "Información de contacto";
            case "section3-title" -> "Ubicación";
            case "section3-text" -> "Descripción de ubicación";
            case "mission-text" -> "Misión de la empresa";
            case "vision-text" -> "Visión de la empresa";
            case "contact-address" -> "Dirección no especificada";
            case "contact-phone" -> "Teléfono no especificado";
            case "contact-email" -> "Email no especificado";
            case "cta-text" -> "Contáctenos";
            case "cta-button" -> "Escribir";
            default -> "Contenido predeterminado";
        };
        if (pc != null) {
            pc.setContent(defaultContent);
            pageContentRepository.save(pc);
        }
        return Map.of("content", defaultContent);
    }

    // ============== CONFIG DE TARIFAS (EDITOR) ==============

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
