package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.*;
import com.example.MardiqueWeb.Repository.DocumentRepository;
import com.example.MardiqueWeb.Repository.PaymentRepository;
import com.example.MardiqueWeb.Repository.SystemConfigRepository;
import com.example.MardiqueWeb.Repository.SolicitudRepository;
import com.example.MardiqueWeb.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    private final Path uploadDir = Paths.get("uploads/comprobantes");

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "gif", "webp");

    private boolean allowedFile(String filename) {
        if (filename == null || filename.isEmpty()) return false;
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    @GetMapping("/inicio")
    public String inicio(Model model, Authentication auth) {
        String username = auth.getName();
        List<Payment> payments = paymentRepository.findByUsername(username);
        List<Solicitud> solicitudes = solicitudRepository.findByUsername(username);
        model.addAttribute("paymentCount", payments.size());
        model.addAttribute("processedCount", payments.stream().filter(Payment::isProcessed).count());
        model.addAttribute("solicitudCount", solicitudes.size());
        model.addAttribute("pendingCount", solicitudes.stream().filter(s -> "PENDIENTE".equals(s.getEstado())).count());
        return "UserInicio";
    }

    @GetMapping("/servicios")
    public String servicios() {
        return "UserServicios";
    }

    @GetMapping("/tramites")
    public String tramites(Model model) {
        Map<String, Document> cardDocs = documentRepository.findByTipoAndCardKeyIsNotNull("TRAMITE")
            .stream().collect(Collectors.toMap(Document::getCardKey, d -> d));
        model.addAttribute("cardDocs", cardDocs);
        model.addAttribute("tramiteCards", List.of(
            Map.of("key", "INSCRIPCION_CLIENTES", "label", "Inscripci\u00f3n de Clientes"),
            Map.of("key", "DECLARACION_LAFT", "label", "Declaraci\u00f3n Prevenci\u00f3n LAFT"),
            Map.of("key", "CERTIFICACION_LAFT", "label", "Certificaci\u00f3n Prevenci\u00f3n LAFT"),
            Map.of("key", "ACUERDO_SEGURIDAD", "label", "Acuerdo de Seguridad"),
            Map.of("key", "SOLICITUDES_PROGRAMABLES", "label", "Solicitudes Programables"),
            Map.of("key", "SOLICITUD_EMBARQUE", "label", "Solicitud de Embarque"),
            Map.of("key", "BOOKING", "label", "Formato Booking"),
            Map.of("key", "AUTORIZACION_INGRESO", "label", "Autorizaci\u00f3n Ingreso/Salida")
        ));
        return "UserTramites";
    }

    @GetMapping("/tarifas")
    public String tarifas(Model model) {
        model.addAttribute("tarifas", documentRepository.findByTipo("TARIFA"));
        model.addAttribute("tarifaIframeUrl", transformIframeUrl(systemConfigRepository.findByConfigKey("TARIFA_IFRAME_URL").map(SystemConfig::getConfigValue).orElse("")));
        model.addAttribute("tarifaPhone", systemConfigRepository.findByConfigKey("TARIFA_PHONE").map(SystemConfig::getConfigValue).orElse("(57) (5) 669 0730"));
        model.addAttribute("tarifaEmail", systemConfigRepository.findByConfigKey("TARIFA_EMAIL").map(SystemConfig::getConfigValue).orElse("info@spmardique.com"));
        return "UserTarifas";
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

    @GetMapping("/payments")
    public String listPayments(Model model, Authentication auth) {
        List<Payment> payments = paymentRepository.findByUsername(auth.getName());
        model.addAttribute("payments", payments);
        return "UserPayments";
    }

    @PostMapping("/payment/register")
    public String registerPayment(@RequestParam String concepto,
                                   @RequestParam String monto,
                                   @RequestParam String referencia,
                                   @RequestParam("fechaPago") String fechaPago,
                                   @RequestParam("comprobante") MultipartFile comprobante,
                                   @RequestParam(defaultValue = "COP") String moneda,
                                   Authentication auth,
                                   RedirectAttributes ra) {
        Payment payment = new Payment();
        payment.setUsername(auth.getName());
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser != null) {
            payment.setEmail(currentUser.getEmail());
        }
        payment.setConcepto(concepto);
        try {
            payment.setMonto(new java.math.BigDecimal(monto));
        } catch (NumberFormatException e) {
            ra.addFlashAttribute("error", "Monto inv\u00e1lido");
            return "redirect:/user/payments";
        }
        payment.setReferencia(referencia);
        try {
            payment.setFechaPago(LocalDate.parse(fechaPago));
        } catch (java.time.format.DateTimeParseException e) {
            ra.addFlashAttribute("error", "Fecha de pago inv\u00e1lida");
            return "redirect:/user/payments";
        }
        payment.setMoneda(moneda);
        if (!comprobante.isEmpty()) {
            if (!allowedFile(comprobante.getOriginalFilename())) {
                ra.addFlashAttribute("error", "Tipo de archivo no permitido (pdf, png, jpg, gif, webp)");
                return "redirect:/user/payments";
            }
            try {
                Files.createDirectories(uploadDir);
                String fileName = System.currentTimeMillis() + "_" + comprobante.getOriginalFilename();
                Path targetPath = uploadDir.resolve(fileName);
                Files.copy(comprobante.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                payment.setComprobanteUsuario("/uploads/comprobantes/" + fileName);
            } catch (IOException e) {
                ra.addFlashAttribute("error", "Error al subir comprobante");
                return "redirect:/user/payments";
            }
        }
        paymentRepository.save(payment);
        ra.addFlashAttribute("success", "Pago registrado correctamente");
        return "redirect:/user/payments";
    }

    @GetMapping("/solicitudes")
    public String listSolicitudes(Model model, Authentication auth) {
        List<Solicitud> list = solicitudRepository.findByUsername(auth.getName());
        model.addAttribute("solicitudes", list);
        return "UserSolicitudes";
    }

    @PostMapping("/solicitudes/create")
    public String createSolicitud(@RequestParam String tipo,
                                   @RequestParam String titulo,
                                   @RequestParam String descripcion,
                                   @RequestParam(required = false) String departamento,
                                   Authentication auth,
                                   RedirectAttributes ra) {
        Solicitud s = new Solicitud();
        s.setUsername(auth.getName());
        s.setEmail(userRepository.findByUsername(auth.getName()).map(User::getEmail).orElse(""));
        s.setTipo(tipo);
        s.setTitulo(titulo);
        s.setDescripcion(descripcion);
        s.setDepartamento(departamento);
        s.setEstado("PENDIENTE");
        solicitudRepository.save(s);
        ra.addFlashAttribute("success", "Solicitud creada correctamente");
        return "redirect:/user/solicitudes";
    }

    @GetMapping("/profile")
    public String profile(Model model, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("User not found: " + auth.getName()));
        model.addAttribute("profileUser", user);
        return "UserProfile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String email,
                                  @RequestParam String nombres,
                                  @RequestParam String apellidos,
                                  @RequestParam(required = false) String telefono,
                                  Authentication auth,
                                  RedirectAttributes ra) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow(() -> new RuntimeException("User not found: " + auth.getName()));
        user.setEmail(email);
        user.setNombres(nombres);
        user.setApellidos(apellidos);
        user.setTelefono(telefono);
        userRepository.save(user);
        ra.addFlashAttribute("success", "Perfil actualizado");
        return "redirect:/user/profile";
    }
}
