package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.Contact;
import com.example.MardiqueWeb.Entity.Document;
import com.example.MardiqueWeb.Entity.GalleryImage;
import com.example.MardiqueWeb.Entity.PageContent;
import com.example.MardiqueWeb.Entity.SupportTicket;
import com.example.MardiqueWeb.Entity.SystemConfig;
import com.example.MardiqueWeb.Entity.User;
import com.example.MardiqueWeb.Entity.TicketHistorial;
import com.example.MardiqueWeb.Repository.*;
import com.example.MardiqueWeb.Service.CarouselService;
import com.example.MardiqueWeb.Service.PageMediaService;
import com.example.MardiqueWeb.Service.PqrsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class PaginasController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PageContentRepository pageContentRepository;

    @Autowired
    private CarouselService carouselService;

    @Autowired
    private PageMediaService pageMediaService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private GalleryImageRepository galleryImageRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private PqrsService pqrsService;

    @Autowired
    private TicketHistorialRepository ticketHistorialRepository;

    @Autowired
    private TicketAdjuntoRepository ticketAdjuntoRepository;

    private static final Set<String> VALID_DOC_TYPES = Set.of("CC", "CE", "NIT", "PA", "DE");
    private static final Set<String> VALID_PETITION_TYPES = Set.of("Petici\u00f3n", "Queja", "Reclamo", "Solicitud");
    private static final Set<String> VALID_CATEGORIES = Set.of("CLIENTE", "PROVEEDOR", "AMBOS");
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "123456", "password123", "admin123", "qwerty", "abc123",
            "letmein", "welcome", "monkey", "dragon", "master", "sunshine",
            "contrase\u00f1a", "123456789", "12345678", "111111", "000000"
    );

    private String sanitize(String input) {
        return input != null ? input.trim() : "";
    }

    @GetMapping("/sesion-activa")
    public String sesionActiva() {
        return "SesionActiva";
    }

    @GetMapping({"/", "/inicio"})
    public String inicio(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("inicio"));
        model.addAttribute("carouselJson", carouselService.getCarouselData());
        model.addAttribute("mediaVideo", pageMediaService.resolvedUrl("inicio", "video"));
        model.addAttribute("ctaImage", pageMediaService.resolvedUrl("inicio", "cta"));
        return "Inicio";
    }

    @GetMapping("/empresa")
    public String empresa(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("empresa"));
        model.addAttribute("heroImage", pageMediaService.resolvedUrl("empresa", "hero"));
        model.addAttribute("visionImage", pageMediaService.resolvedUrl("empresa", "vision"));
        model.addAttribute("introImage", pageMediaService.resolvedUrl("empresa", "intro"));
        model.addAttribute("ctaImage", pageMediaService.resolvedUrl("empresa", "cta"));
        return "Empresa";
    }

    @GetMapping("/servicios")
    public String servicios(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("servicios"));
        model.addAttribute("carouselJson", carouselService.getCarouselData());
        model.addAttribute("heroImage", pageMediaService.resolvedUrl("servicios", "hero"));
        model.addAttribute("ctaImage", pageMediaService.resolvedUrl("servicios", "cta"));
        return "Servicios";
    }

    @GetMapping("/procedimientos")
    public String procedimientos(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("procedimientos"));
        model.addAttribute("docs", publicDocs(documentRepository.findByTipo("PROCEDIMIENTO")));
        model.addAttribute("heroImage", pageMediaService.resolvedUrl("procedimientos", "hero"));
        model.addAttribute("ctaImage", pageMediaService.resolvedUrl("procedimientos", "cta"));
        return "Procedimientos";
    }

    @GetMapping("/tarifas")
    public String tarifas(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("tarifas"));
        model.addAttribute("docs", publicDocs(documentRepository.findByTipo("TARIFA")));
        model.addAttribute("reglamentoDoc", documentRepository.findByTipoAndCardKey("TARIFA", "REGLAMENTO").orElse(null));
        model.addAttribute("tarifaIframeUrl", transformIframeUrl(systemConfigRepository.findByConfigKey("TARIFA_IFRAME_URL").map(SystemConfig::getConfigValue).orElse("")));
        model.addAttribute("tarifaPhone", systemConfigRepository.findByConfigKey("TARIFA_PHONE").map(SystemConfig::getConfigValue).orElse("(57) (5) 669 0730"));
        model.addAttribute("tarifaEmail", systemConfigRepository.findByConfigKey("TARIFA_EMAIL").map(SystemConfig::getConfigValue).orElse("info@spmardique.com"));
        model.addAttribute("heroImage", pageMediaService.resolvedUrl("tarifas", "hero"));
        model.addAttribute("ctaImage", pageMediaService.resolvedUrl("tarifas", "cta"));
        return "Tarifas";
    }

    @GetMapping("/tramites-en-linea")
    public String tramitesEnLinea(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("tramites"));
        model.addAttribute("docs", publicDocs(documentRepository.findByTipo("TRAMITE")));
        Map<String, Document> cardDocs = loadCardDocs("TRAMITE");
        cardDocs.values().removeIf(d -> d.getDestinatarios() != null && !d.getDestinatarios().isEmpty());
        model.addAttribute("cardDocs", cardDocs);
        model.addAttribute("reglamentoDoc", documentRepository.findByTipoAndCardKey("TRAMITE", "REGLAMENTO").orElse(null));
        model.addAttribute("heroImage", pageMediaService.resolvedUrl("tramites", "hero"));
        model.addAttribute("ctaImage", pageMediaService.resolvedUrl("tramites", "cta"));
        return "Tramitesenlinea";
    }

    @GetMapping("/galeria")
    public String galeria(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("galeria"));
        model.addAttribute("images", galleryImageRepository.findByActiveTrue());
        model.addAttribute("heroImage", pageMediaService.resolvedUrl("galeria", "hero"));
        model.addAttribute("ctaImage", pageMediaService.resolvedUrl("galeria", "cta"));
        return "Galeria";
    }

    @GetMapping("/contacto")
    public String contacto(Authentication auth, Model model,
                           @RequestParam(required = false) String radicado,
                           @RequestParam(required = false) String documento) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("contacto"));
        model.addAttribute("contacts", contactRepository.findAll());
        model.addAttribute("heroImage", pageMediaService.resolvedUrl("contacto", "hero"));
        model.addAttribute("ctaImage", pageMediaService.resolvedUrl("contacto", "cta"));
        model.addAttribute("pqrsService", pqrsService);
        if (radicado != null && !radicado.trim().isEmpty() && documento != null && !documento.trim().isEmpty()) {
            SupportTicket ticket = supportTicketRepository.findByRadicado(radicado.trim().toUpperCase()).orElse(null);
            if (ticket != null && ticket.getNumeroDocumento() != null
                    && ticket.getNumeroDocumento().equalsIgnoreCase(documento.trim())) {
                model.addAttribute("ticketConsulta", ticket);
                model.addAttribute("historialConsulta", ticketHistorialRepository.findByTicketIdOrderByFechaAsc(ticket.getId()));
                model.addAttribute("adjuntosConsulta", ticketAdjuntoRepository.findByTicketIdOrderByFechaAsc(ticket.getId()));
            } else {
                model.addAttribute("segError", "No se encontr\u00f3 una PQRS con esos datos. Verifique el radicado y el documento.");
            }
        }
        return "Contacto";
    }

    @PostMapping("/contacto/pqrs")
    public String submitPQRS(@RequestParam String tipoDocumento, @RequestParam String numeroDocumento,
                             @RequestParam String nombreCompleto, @RequestParam String email,
                             @RequestParam String telefono, @RequestParam String tipoPeticion,
                             @RequestParam String departamento, @RequestParam String descripcion,
                             @RequestParam(value = "adjuntos", required = false) MultipartFile[] adjuntos,
                             RedirectAttributes ra) {
        if (!VALID_DOC_TYPES.contains(tipoDocumento)) {
            ra.addFlashAttribute("pqrsError", "Tipo de documento inv\u00e1lido");
            return "redirect:/contacto#pqrs";
        }
        if (!VALID_PETITION_TYPES.contains(tipoPeticion)) {
            ra.addFlashAttribute("pqrsError", "Tipo de petici\u00f3n inv\u00e1lido");
            return "redirect:/contacto#pqrs";
        }
        if (numeroDocumento == null || !numeroDocumento.trim().matches("^[A-Za-z0-9\\-]{4,20}$")) {
            ra.addFlashAttribute("pqrsError", "N\u00famero de documento inv\u00e1lido");
            return "redirect:/contacto#pqrs";
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            ra.addFlashAttribute("pqrsError", "Email inv\u00e1lido");
            return "redirect:/contacto#pqrs";
        }
        if (descripcion == null || descripcion.trim().length() < 10) {
            ra.addFlashAttribute("pqrsError", "La descripci\u00f3n debe tener al menos 10 caracteres");
            return "redirect:/contacto#pqrs";
        }
        SupportTicket ticket = new SupportTicket();
        ticket.setTipoDocumento(sanitize(tipoDocumento));
        ticket.setNumeroDocumento(sanitize(numeroDocumento));
        ticket.setNombreCompleto(sanitize(nombreCompleto));
        ticket.setEmail(sanitize(email));
        ticket.setTelefono(sanitize(telefono));
        ticket.setTipoPeticion(sanitize(tipoPeticion));
        ticket.setDepartamento(sanitize(departamento));
        ticket.setMessage(descripcion != null ? descripcion.trim() : "");
        ticket.setSubject("PQRS: " + sanitize(tipoPeticion));
        ticket.setUsername(sanitize(nombreCompleto));
        ticket.setOrigen("PQRS");
        ticket.setStatus("ABIERTO");
        SupportTicket saved = pqrsService.radicarTicket(ticket);
        List<MultipartFile> archivos = new ArrayList<>();
        if (adjuntos != null) {
            for (MultipartFile f : adjuntos) {
                if (f != null && !f.isEmpty()) {
                    archivos.add(f);
                }
            }
        }
        pqrsService.guardarAdjuntos(saved.getId(), archivos, "SOLICITANTE");
        ra.addFlashAttribute("pqrsSuccess",
                "PQRS radicada exitosamente con radicado <strong>" + saved.getRadicado() + "</strong>. "
                        + "Recibir\u00e1 confirmaci\u00f3n en su correo y puede darle seguimiento en el formulario de consulta.");
        return "redirect:/contacto#pqrs";
    }

    @PostMapping("/contacto/pqrs/seguimiento")
    public String consultarPqrs(@RequestParam String radicado,
                                @RequestParam String numeroDocumento,
                                RedirectAttributes ra) {
        if (radicado == null || radicado.trim().isEmpty() || numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            ra.addFlashAttribute("segError", "Ingrese el radicado y el n\u00famero de documento");
            return "redirect:/contacto#seguimiento";
        }
        SupportTicket ticket = supportTicketRepository.findByRadicado(radicado.trim().toUpperCase()).orElse(null);
        if (ticket == null || ticket.getNumeroDocumento() == null || !ticket.getNumeroDocumento().equalsIgnoreCase(numeroDocumento.trim())) {
            ra.addFlashAttribute("segError", "No se encontr\u00f3 una PQRS con esos datos. Verifique el radicado y el documento.");
            return "redirect:/contacto#seguimiento";
        }
        try {
            return "redirect:/contacto?radicado=" + java.net.URLEncoder.encode(radicado.trim().toUpperCase(), java.nio.charset.StandardCharsets.UTF_8)
                    + "&documento=" + java.net.URLEncoder.encode(numeroDocumento.trim(), java.nio.charset.StandardCharsets.UTF_8)
                    + "#seguimiento";
        } catch (Exception e) {
            return "redirect:/contacto#seguimiento";
        }
    }

    @PostMapping("/contacto/message")
    public String submitContactMessage(@RequestParam String nombre,
                                       @RequestParam String email,
                                       @RequestParam String telefono,
                                       @RequestParam(required = false, defaultValue = "General") String asunto,
                                       @RequestParam String mensaje,
                                       RedirectAttributes ra) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            ra.addFlashAttribute("mensajeError", "Email inv\u00e1lido");
            return "redirect:/contacto#contactenos";
        }
        SupportTicket ticket = new SupportTicket();
        ticket.setNombreCompleto(sanitize(nombre));
        ticket.setEmail(sanitize(email));
        ticket.setTelefono(sanitize(telefono));
        ticket.setSubject(sanitize(asunto));
        ticket.setMessage(mensaje != null ? mensaje.trim() : "");
        ticket.setUsername(sanitize(nombre));
        ticket.setOrigen("CONTACTO");
        ticket.setStatus("ABIERTO");
        supportTicketRepository.save(ticket);
        ra.addFlashAttribute("mensajeSuccess", "Mensaje enviado con \u00e9xito. Nos pondremos en contacto pronto.");
        return "redirect:/contacto#contactenos";
    }

    @GetMapping("/login")
    public String login(Authentication auth, HttpServletRequest request) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/dashboard";
        request.getSession(true);
        return "Login";
    }

    @GetMapping("/register")
    public String register(Authentication auth, Model model, HttpServletRequest request) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/dashboard";
        request.getSession(true);
        model.addAttribute("tipo", "PERSONA");
        return "Register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username, @RequestParam String password,
                             @RequestParam String email, @RequestParam String nombres,
                             @RequestParam String apellidos,
                             @RequestParam(required = false) String telefono,
                             @RequestParam String confirmPassword,
                             @RequestParam(defaultValue = "PERSONA") String tipo,
                             @RequestParam(required = false) String nit,
                             @RequestParam(required = false) String categoria,
                             @RequestParam(required = false) String aceptaTerminos,
                             Model model) {
        model.addAttribute("nombres", nombres);
        model.addAttribute("apellidos", apellidos);
        model.addAttribute("email", email);
        model.addAttribute("username", username);
        model.addAttribute("telefono", telefono);
        model.addAttribute("tipo", tipo);
        model.addAttribute("nit", nit);
        model.addAttribute("categoria", categoria);
        if (!"on".equals(aceptaTerminos)) {
            model.addAttribute("error", "Debe aceptar los T\u00e9rminos y Condiciones");
            return "Register";
        }
        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "El usuario ya existe");
            return "Register";
        }
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            model.addAttribute("error", "El usuario solo puede contener letras, números y guiones bajos (3-20 caracteres)");
            return "Register";
        }
        if (email != null && userRepository.existsByEmail(email)) {
            model.addAttribute("error", "El email ya está registrado");
            return "Register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contrase\u00f1as no coinciden");
            return "Register";
        }
        if (password.length() < 8) {
            model.addAttribute("error", "La contrase\u00f1a debe tener al menos 8 caracteres");
            return "Register";
        }
        if (!password.matches(".*[A-Z].*")) {
            model.addAttribute("error", "La contrase\u00f1a debe contener al menos una may\u00fascula");
            return "Register";
        }
        if (!password.matches(".*[0-9].*")) {
            model.addAttribute("error", "La contrase\u00f1a debe contener al menos un n\u00famero");
            return "Register";
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            model.addAttribute("error", "La contrase\u00f1a debe contener al menos un car\u00e1cter especial");
            return "Register";
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            model.addAttribute("error", "Contrase\u00f1a muy com\u00fan, elige otra");
            return "Register";
        }
        if (telefono != null && !telefono.isEmpty() && !telefono.matches("^[0-9+\\-\\s()]{7,20}$")) {
            model.addAttribute("error", "Tel\u00e9fono inv\u00e1lido");
            return "Register";
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            model.addAttribute("error", "Email inv\u00e1lido");
            return "Register";
        }
        if (nit == null || !nit.trim().matches("^[A-Za-z0-9\\-]{4,15}$")) {
            model.addAttribute("error", "Documento inv\u00e1lido (solo letras, n\u00fameros y guiones)");
            return "Register";
        }
        if ("CLIENTE".equals(tipo) && (categoria == null || !VALID_CATEGORIES.contains(categoria))) {
            model.addAttribute("error", "Categor\u00eda inv\u00e1lida para Cliente/Proveedor");
            return "Register";
        }
        User user = new User(username, passwordEncoder.encode(password), email, nombres, apellidos, telefono, "ROLE_USER");
        user.setTipo(tipo);
        user.setNit(nit.trim());
        user.setCategoria(categoria);
        userRepository.save(user);
        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        if (auth == null) return "redirect:/login";
        String role = auth.getAuthorities().stream().findFirst()
                .map(g -> g.getAuthority()).orElse("ROLE_USER");
        return switch (role) {
            case "ROLE_ADMIN" -> "redirect:/admin/dashboard";
            case "ROLE_EDITOR" -> "redirect:/editor/dashboard";
            default -> "redirect:/user/inicio";
        };
    }

    private Map<String, String> loadPageContents(String page) {
        List<PageContent> contents = pageContentRepository.findByPage(page);
        return contents.stream().filter(c -> c.getSectionKey() != null)
                .collect(Collectors.toMap(PageContent::getSectionKey, c -> c.getContent() == null ? "" : c.getContent(), (a, b) -> a));
    }

    private Map<String, Document> loadCardDocs(String tipo) {
        List<Document> docs = documentRepository.findByTipoAndCardKeyIsNotNull(tipo);
        return docs.stream().filter(d -> d.getCardKey() != null)
                .collect(Collectors.toMap(Document::getCardKey, d -> d, (a, b) -> a));
    }

    private List<Document> publicDocs(List<Document> docs) {
        return docs.stream().filter(d -> d.getDestinatarios() == null || d.getDestinatarios().isEmpty()).collect(Collectors.toList());
    }

    private String transformIframeUrl(String url) {
        if (url == null || url.isBlank()) return "";
        String trimmed = url.trim();
        // Solo se permiten URLs http/https: bloquea javascript:, data:, vbscript:, etc.
        if (!trimmed.toLowerCase().startsWith("https://") && !trimmed.toLowerCase().startsWith("http://")) {
            return "";
        }
        if (trimmed.contains("drive.google.com") && trimmed.contains("view?usp=sharing")) {
            return trimmed.replace("view?usp=sharing", "preview");
        }
        if (trimmed.contains("drive.google.com") && trimmed.contains("view?usp=drivesdk")) {
            return trimmed.replace("view?usp=drivesdk", "preview");
        }
        return trimmed;
    }
}