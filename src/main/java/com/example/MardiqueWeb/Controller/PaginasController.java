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

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        SupportTicket ticket = new SupportTicket();
        ticket.setTipoDocumento(tipoDocumento);
        ticket.setNumeroDocumento(numeroDocumento);
        ticket.setNombreCompleto(nombreCompleto);
        ticket.setEmail(email);
        ticket.setTelefono(telefono);
        ticket.setTipoPeticion(tipoPeticion);
        ticket.setDepartamento(departamento);
        ticket.setMessage(descripcion);
        ticket.setSubject("PQRS: " + tipoPeticion);
        ticket.setUsername(nombreCompleto);
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
        SupportTicket ticket = new SupportTicket();
        ticket.setNombreCompleto(nombre);
        ticket.setEmail(email);
        ticket.setTelefono(telefono);
        ticket.setSubject(asunto);
        ticket.setMessage(mensaje);
        ticket.setUsername(nombre);
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
    public String register(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/dashboard";
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
                             Model model) {
        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "El usuario ya existe");
            return "Register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "Register";
        }
        if (nit == null || nit.trim().isEmpty()) {
            model.addAttribute("error", "El documento es obligatorio");
            return "Register";
        }
        if ("CLIENTE".equals(tipo) && (categoria == null || categoria.isEmpty())) {
            model.addAttribute("error", "La categor\u00eda es obligatoria para Cliente/Proveedor");
            return "Register";
        }
        User user = new User(username, passwordEncoder.encode(password), email, nombres, apellidos, telefono, "ROLE_USER");
        user.setTipo(tipo);
        user.setNit(nit);
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
