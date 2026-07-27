package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.*;
import com.example.MardiqueWeb.Repository.*;
import com.example.MardiqueWeb.Service.AuditService;
import com.example.MardiqueWeb.Service.CloudinaryService;
import com.example.MardiqueWeb.Service.EncryptionService;
import com.example.MardiqueWeb.Service.PdfService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private GalleryImageRepository galleryImageRepository;

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private SystemConfigRepository systemConfigRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EncryptionService encryptionService;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "gif", "webp");

    private boolean allowedFile(String filename) {
        if (filename == null || filename.isEmpty()) return false;
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    @ModelAttribute
    public void addAdminAttributes(Model model, Authentication auth) {
        if (auth != null) {
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null) {
                model.addAttribute("isFullAdmin", user.getDepartamento() == null);
                model.addAttribute("userDepartamento", user.getDepartamento());
            } else {
                model.addAttribute("isFullAdmin", true);
                model.addAttribute("userDepartamento", null);
            }
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        boolean isFullAdmin = currentUser == null || currentUser.getDepartamento() == null;
        String dept = currentUser != null ? currentUser.getDepartamento() : null;

        model.addAttribute("totalUsers", isFullAdmin ? userRepository.count() : 0);
        model.addAttribute("totalContacts", contactRepository.count());
        model.addAttribute("totalDocuments", isFullAdmin ? documentRepository.count() : 0);
        model.addAttribute("totalGallery", isFullAdmin ? galleryImageRepository.count() : 0);
        model.addAttribute("totalMensajes", supportTicketRepository.countByOrigen("CONTACTO"));

        if (isFullAdmin) {
            model.addAttribute("pendingSolicitudes", solicitudRepository.countByEstado("PENDIENTE"));
        } else {
            model.addAttribute("pendingSolicitudes",
                solicitudRepository.findByDepartamento(dept).stream()
                    .filter(s -> "PENDIENTE".equals(s.getEstado())).count());
        }

        model.addAttribute("recentUsers", isFullAdmin
            ? userRepository.findAll().stream().limit(5).toList()
            : List.of());
        return "AdminDashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser != null && currentUser.getDepartamento() != null) {
            return "redirect:/admin/dashboard";
        }
        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("activeUsers", allUsers.stream().filter(User::isEnabled).count());
        model.addAttribute("empresaUsers", allUsers.stream().filter(u -> "EMPRESA".equals(u.getTipo())).count());
        model.addAttribute("clienteUsers", allUsers.stream().filter(u -> "CLIENTE".equals(u.getTipo())).count());
        return "AdminUsers";
    }

    @PostMapping("/users/toggle/{id}")
    public String toggleUser(@PathVariable Long id, HttpServletRequest request, Authentication auth, RedirectAttributes ra) {
        User currentAdmin = userRepository.findByUsername(auth.getName()).orElse(null);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (currentAdmin != null && currentAdmin.getDepartamento() != null) {
            throw new AccessDeniedException("No tienes permiso para modificar este usuario");
        }
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        String action = user.isEnabled() ? "USER_ENABLE" : "USER_DISABLE";
        auditService.log(auth.getName(), action, "User " + user.getUsername() + " " + (user.isEnabled() ? "activated" : "deactivated"), request);
        ra.addFlashAttribute("success", "Usuario " + (user.isEnabled() ? "activado" : "desactivado"));
        return "redirect:/admin/users";
    }

    @PostMapping("/users/role/{id}")
    public String changeRole(@PathVariable Long id, @RequestParam String role, HttpServletRequest request, Authentication auth, RedirectAttributes ra) {
        User currentAdmin = userRepository.findByUsername(auth.getName()).orElse(null);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (currentAdmin != null && currentAdmin.getDepartamento() != null) {
            throw new AccessDeniedException("No tienes permiso para cambiar roles");
        }
        String oldRole = user.getRole();
        user.setRole(role);
        userRepository.save(user);
        auditService.log(auth.getName(), "ROLE_CHANGE", "User " + user.getUsername() + " role changed from " + oldRole + " to " + role, request);
        ra.addFlashAttribute("success", "Rol actualizado");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/update/{id}")
    public String updateUser(@PathVariable Long id, @RequestParam String nombres,
                              @RequestParam String apellidos, @RequestParam String email,
                              @RequestParam String telefono, @RequestParam String tipo,
                              @RequestParam(required = false) String nit,
                              @RequestParam(required = false) String categoria,
                              HttpServletRequest request, Authentication auth,
                              RedirectAttributes ra) {
        User currentAdmin = userRepository.findByUsername(auth.getName()).orElse(null);
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (currentAdmin != null && currentAdmin.getDepartamento() != null) {
            throw new AccessDeniedException("No tienes permiso para modificar este usuario");
        }
        if (email != null && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            ra.addFlashAttribute("error", "El formato del email no es válido");
            return "redirect:/admin/users";
        }
        user.setNombres(nombres);
        user.setApellidos(apellidos);
        user.setEmail(email);
        user.setTelefono(telefono);
        user.setTipo(tipo);
        user.setNit(nit);
        user.setCategoria(categoria);
        userRepository.save(user);
        auditService.log(auth.getName(), "USER_UPDATE", "Updated user " + user.getUsername(), request);
        ra.addFlashAttribute("success", "Usuario actualizado");
        return "redirect:/admin/users";
    }

    @GetMapping("/solicitudes")
    public String listSolicitudes(Model model, Authentication auth,
                                   @RequestParam(required = false) String estado,
                                   @RequestParam(required = false) String tipo,
                                   @RequestParam(required = false) String search) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        boolean isFullAdmin = currentUser == null || currentUser.getDepartamento() == null;
        String dept = currentUser != null ? currentUser.getDepartamento() : null;

        List<Solicitud> allSol = isFullAdmin
            ? solicitudRepository.findAll()
            : solicitudRepository.findByDepartamento(dept);
        var solStream = allSol.stream();
        if (estado != null && !estado.isEmpty()) {
            solStream = solStream.filter(s -> estado.equals(s.getEstado()));
        }
        if (tipo != null && !tipo.isEmpty()) {
            solStream = solStream.filter(s -> tipo.equals(s.getTipo()));
        }
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            solStream = solStream.filter(s -> (s.getUsername() != null && s.getUsername().toLowerCase().contains(q))
                                           || (s.getTitulo() != null && s.getTitulo().toLowerCase().contains(q)));
        }
        List<SupportTicket> allTickets = supportTicketRepository.findAllByOrderByCreatedAtDesc();
        var tktStream = allTickets.stream().filter(t -> "PQRS".equals(t.getOrigen()));
        if (!isFullAdmin) {
            tktStream = tktStream.filter(t -> dept != null && dept.equals(t.getDepartamento()));
        }
        if (estado != null && !estado.isEmpty()) {
            tktStream = tktStream.filter(t -> estado.equals(t.getStatus()));
        }
        if (tipo != null && !tipo.isEmpty()) {
            tktStream = tktStream.filter(t -> tipo.equals(t.getTipoPeticion()));
        }
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            tktStream = tktStream.filter(t -> (t.getNombreCompleto() != null && t.getNombreCompleto().toLowerCase().contains(q))
                                            || (t.getEmail() != null && t.getEmail().toLowerCase().contains(q)));
        }
        model.addAttribute("solicitudes", solStream.toList());
        model.addAttribute("pqrsList", tktStream.toList());
        model.addAttribute("selEstado", estado);
        model.addAttribute("selTipo", tipo);
        model.addAttribute("search", search);
        return "AdminSolicitudes";
    }

    @PostMapping("/solicitudes/responder/{id}")
    public String responderSolicitud(@PathVariable Long id, @RequestParam String respuesta,
                                      RedirectAttributes ra) {
        SupportTicket ticket = supportTicketRepository.findById(id).orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        ticket.setRespuesta(respuesta);
        ticket.setStatus("CERRADO");
        supportTicketRepository.save(ticket);
        ra.addFlashAttribute("success", "Respuesta enviada al solicitante");
        return "redirect:/admin/solicitudes";
    }

    @PostMapping("/solicitudes/responder-solicitud/{id}")
    public String responderSolicitud(@PathVariable Long id, @RequestParam String estado,
                                      @RequestParam String respuestaAdmin, RedirectAttributes ra) {
        Solicitud s = solicitudRepository.findById(id).orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        s.setEstado(estado);
        s.setRespuestaAdmin(respuestaAdmin);
        solicitudRepository.save(s);
        ra.addFlashAttribute("success", "Respuesta enviada al solicitante");
        return "redirect:/admin/solicitudes";
    }

    @PostMapping("/solicitudes/status/{id}")
    public String updateSolicitudStatus(@PathVariable Long id, @RequestParam String estado, RedirectAttributes ra) {
        Solicitud s = solicitudRepository.findById(id).orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        s.setEstado(estado);
        solicitudRepository.save(s);
        ra.addFlashAttribute("success", "Solicitud actualizada");
        return "redirect:/admin/solicitudes";
    }

    @GetMapping("/payments")
    public String listPayments(Model model,
                                @RequestParam(required = false) String estado,
                                @RequestParam(required = false) String moneda,
                                @RequestParam(required = false) String search) {
        List<Payment> all = paymentRepository.findAll();
        var stream = all.stream();
        if (estado != null && !estado.isEmpty()) {
            boolean proc = "CONFIRMADO".equals(estado);
            stream = stream.filter(p -> p.isProcessed() == proc);
        }
        if (moneda != null && !moneda.isEmpty()) {
            stream = stream.filter(p -> moneda.equals(p.getMoneda()));
        }
        if (search != null && !search.isEmpty()) {
            String q = search.toLowerCase();
            stream = stream.filter(p -> (p.getUsername() != null && p.getUsername().toLowerCase().contains(q))
                                     || (p.getConcepto() != null && p.getConcepto().toLowerCase().contains(q)));
        }
        model.addAttribute("payments", stream.toList());
        model.addAttribute("selEstado", estado);
        model.addAttribute("selMoneda", moneda);
        model.addAttribute("search", search);
        return "AdminPayments";
    }

    @PostMapping("/payments/confirm")
    @Transactional
    public String confirmPayment(@RequestParam Long paymentId,
                                   @RequestParam(defaultValue = "false") boolean generarPdf,
                                   @RequestParam(value = "comprobanteManual", required = false) MultipartFile comprobanteManual,
                                   HttpServletRequest request, Authentication auth,
                                   RedirectAttributes ra) {
        try {
            Payment p = paymentRepository.findById(paymentId).orElseThrow(() -> new RuntimeException("Pago no encontrado"));
            p.setProcessed(true);
            Payment saved = paymentRepository.save(p);

            if (generarPdf) {
                try {
                    byte[] pdfBytes = pdfService.generatePaymentReceipt(saved);
                    String url = cloudinaryService.uploadBytes(pdfBytes, "pago_" + paymentId + "_sistema.pdf");
                    saved.setComprobantePath(url);
                    paymentRepository.save(saved);
                } catch (Exception e) {
                    ra.addFlashAttribute("error", "Pago confirmado pero error al generar PDF: " + e.getMessage());
                    return "redirect:/admin/payments";
                }
            }

            if (comprobanteManual != null && !comprobanteManual.isEmpty()) {
                if (!allowedFile(comprobanteManual.getOriginalFilename())) {
                    ra.addFlashAttribute("error", "Tipo de archivo no permitido (pdf, png, jpg, gif, webp)");
                    return "redirect:/admin/payments";
                }
                try {
                    String url = cloudinaryService.uploadFile(comprobanteManual);
                    saved.setComprobanteManualPath(url);
                    paymentRepository.save(saved);
                } catch (IOException e) {
                    ra.addFlashAttribute("error", "Pago confirmado pero error al guardar archivo manual: " + e.getMessage());
                    return "redirect:/admin/payments";
                }
            }

            auditService.log(auth.getName(), "PAYMENT_CONFIRM", "Payment #" + paymentId + " confirmed", request);
            ra.addFlashAttribute("success", "Pago #" + paymentId + " confirmado exitosamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al confirmar pago: " + e.getMessage());
        }
        return "redirect:/admin/payments";
    }

    @GetMapping("/config")
    public String listConfig(Model model, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser != null && currentUser.getDepartamento() != null) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("configs", systemConfigRepository.findAll());
        model.addAttribute("newConfig", new SystemConfig());
        return "AdminConfig";
    }

    @PostMapping("/config/save")
    public String saveConfig(@ModelAttribute("newConfig") SystemConfig config, HttpServletRequest request, Authentication auth, RedirectAttributes ra) {
        systemConfigRepository.save(config);
        auditService.log(auth.getName(), "CONFIG_CREATE", "Created config " + config.getConfigKey(), request);
        ra.addFlashAttribute("success", "Configuraci&oacute;n guardada");
        return "redirect:/admin/config";
    }

    @PostMapping("/config/update")
    public String updateConfig(@RequestParam Long id, @RequestParam String configValue, HttpServletRequest request, Authentication auth, RedirectAttributes ra) {
        SystemConfig config = systemConfigRepository.findById(id).orElseThrow(() -> new RuntimeException("Config no encontrada"));
        String oldVal = config.getConfigValue();
        config.setConfigValue(configValue);
        systemConfigRepository.save(config);
        auditService.log(auth.getName(), "CONFIG_UPDATE", "Config " + config.getConfigKey() + " updated", request);
        ra.addFlashAttribute("success", "Configuraci&oacute;n actualizada");
        return "redirect:/admin/config";
    }

    @PostMapping("/config/delete/{id}")
    public String deleteConfig(@PathVariable Long id, HttpServletRequest request, Authentication auth, RedirectAttributes ra) {
        SystemConfig config = systemConfigRepository.findById(id).orElse(null);
        systemConfigRepository.deleteById(id);
        if (config != null) {
            auditService.log(auth.getName(), "CONFIG_DELETE", "Deleted config " + config.getConfigKey(), request);
        }
        ra.addFlashAttribute("success", "Configuraci&oacute;n eliminada");
        return "redirect:/admin/config";
    }

    @PostMapping("/migrate-encryption")
    @ResponseBody
    @Transactional
    public String migrateEncryption(Authentication auth) {
        User currentAdmin = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentAdmin == null || currentAdmin.getDepartamento() != null) {
            return "Solo administradores generales pueden migrar";
        }
        List<User> users = userRepository.findAll();
        int count = 0;
        for (User user : users) {
            boolean changed = false;
            if (user.getEmail() != null) { user.setEmail(user.getEmail()); changed = true; }
            if (user.getTelefono() != null) { user.setTelefono(user.getTelefono()); changed = true; }
            if (user.getNit() != null) { user.setNit(user.getNit()); changed = true; }
            if (changed) {
                userRepository.save(user);
                count++;
            }
        }
        return "Migración completada. " + count + " usuarios re-encryptados al nuevo formato AES/GCM.";
    }

    @GetMapping("/mensajes")
    public String listMensajes(Model model) {
        model.addAttribute("mensajes", supportTicketRepository.findByOrigenOrderByCreatedAtDesc("CONTACTO"));
        return "AdminMensajes";
    }
}
