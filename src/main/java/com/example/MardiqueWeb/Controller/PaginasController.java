package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.Contact;
import com.example.MardiqueWeb.Entity.Document;
import com.example.MardiqueWeb.Entity.GalleryImage;
import com.example.MardiqueWeb.Entity.PageContent;
import com.example.MardiqueWeb.Entity.SupportTicket;
import com.example.MardiqueWeb.Entity.SystemConfig;
import com.example.MardiqueWeb.Entity.User;
import com.example.MardiqueWeb.Repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.HtmlUtils;

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
    private DocumentRepository documentRepository;

    @Autowired
    private GalleryImageRepository galleryImageRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    private static final Set<String> VALID_DOC_TYPES = Set.of("CC", "CE", "NIT", "PA", "DE");
    private static final Set<String> VALID_PETITION_TYPES = Set.of("Petici\u00f3n", "Queja", "Reclamo", "Solicitud");
    private static final Set<String> VALID_CATEGORIES = Set.of("CLIENTE", "PROVEEDOR", "AMBOS");
    private static final Set<String> COMMON_PASSWORDS = Set.of(
        "password", "123456", "password123", "admin123", "qwerty", "abc123",
        "letmein", "welcome", "monkey", "dragon", "master", "sunshine",
        "contrase\u00f1a", "123456789", "12345678", "111111", "000000"
    );

    private String sanitize(String input) {
        return input != null ? HtmlUtils.htmlEscape(input.trim()) : "";
    }

    @GetMapping("/sesion-activa")
    public String sesionActiva() {
        return "SesionActiva";
    }

    @GetMapping({"/", "/inicio"})
    public String inicio(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        return "Inicio";
    }

    @GetMapping("/empresa")
    public String empresa(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        return "Empresa";
    }

    @GetMapping("/servicios")
    public String servicios(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        return "Servicios";
    }

    @GetMapping("/procedimientos")
    public String procedimientos(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("procedimientos"));
        model.addAttribute("docs", documentRepository.findByTipo("PROCEDIMIENTO"));
        return "Procedimientos";
    }

    @GetMapping("/tarifas")
    public String tarifas(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("tarifas"));
        model.addAttribute("docs", documentRepository.findByTipo("TARIFA"));
        model.addAttribute("reglamentoDoc", documentRepository.findByTipoAndCardKey("TARIFA", "REGLAMENTO").orElse(null));
        model.addAttribute("tarifaIframeUrl", transformIframeUrl(systemConfigRepository.findByConfigKey("TARIFA_IFRAME_URL").map(SystemConfig::getConfigValue).orElse("")));
        model.addAttribute("tarifaPhone", systemConfigRepository.findByConfigKey("TARIFA_PHONE").map(SystemConfig::getConfigValue).orElse("(57) (5) 669 0730"));
        model.addAttribute("tarifaEmail", systemConfigRepository.findByConfigKey("TARIFA_EMAIL").map(SystemConfig::getConfigValue).orElse("info@spmardique.com"));
        return "Tarifas";
    }

    @GetMapping("/tramites-en-linea")
    public String tramitesEnLinea(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("tramites"));
        model.addAttribute("docs", documentRepository.findByTipo("TRAMITE"));
        model.addAttribute("cardDocs", loadCardDocs("TRAMITE"));
        model.addAttribute("reglamentoDoc", documentRepository.findByTipoAndCardKey("TRAMITE", "REGLAMENTO").orElse(null));
        return "Tramitesenlinea";
    }

    @GetMapping("/galeria")
    public String galeria(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("galeria"));
        model.addAttribute("images", galleryImageRepository.findByActiveTrue());
        return "Galeria";
    }

    @GetMapping("/contacto")
    public String contacto(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/sesion-activa";
        model.addAttribute("pageContents", loadPageContents("contacto"));
        model.addAttribute("contacts", contactRepository.findAll());
        return "Contacto";
    }

    @PostMapping("/contacto/pqrs")
    public String submitPQRS(@RequestParam String tipoDocumento, @RequestParam String numeroDocumento,
                              @RequestParam String nombreCompleto, @RequestParam String email,
                              @RequestParam String telefono, @RequestParam String tipoPeticion,
                              @RequestParam String departamento, @RequestParam String descripcion,
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
        SupportTicket ticket = new SupportTicket();
        ticket.setTipoDocumento(sanitize(tipoDocumento));
        ticket.setNumeroDocumento(sanitize(numeroDocumento));
        ticket.setNombreCompleto(sanitize(nombreCompleto));
        ticket.setEmail(sanitize(email));
        ticket.setTelefono(sanitize(telefono));
        ticket.setTipoPeticion(sanitize(tipoPeticion));
        ticket.setDepartamento(sanitize(departamento));
        ticket.setMessage(HtmlUtils.htmlEscape(descripcion));
        ticket.setSubject("PQRS: " + sanitize(tipoPeticion));
        ticket.setUsername(sanitize(nombreCompleto));
        ticket.setOrigen("PQRS");
        ticket.setStatus("ABIERTO");
        supportTicketRepository.save(ticket);
        ra.addFlashAttribute("pqrsSuccess", "PQRS radicada exitosamente. Recibir\u00e1 confirmaci\u00f3n en su correo.");
        return "redirect:/contacto#pqrs";
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
        ticket.setMessage(HtmlUtils.htmlEscape(mensaje));
        ticket.setUsername(sanitize(nombre));
        ticket.setOrigen("CONTACTO");
        ticket.setStatus("ABIERTO");
        supportTicketRepository.save(ticket);
        ra.addFlashAttribute("mensajeSuccess", "Mensaje enviado con \u00e9xito. Nos pondremos en contacto pronto.");
        return "redirect:/contacto#contactenos";
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/dashboard";
        return "Login";
    }

    @GetMapping("/register")
    public String register(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/dashboard";
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

    private String transformIframeUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        if (url.contains("drive.google.com") && url.contains("view?usp=sharing")) {
            return url.replace("view?usp=sharing", "preview");
        }
        if (url.contains("drive.google.com") && url.contains("view?usp=drivesdk")) {
            return url.replace("view?usp=drivesdk", "preview");
        }
        return url;
    }
}
