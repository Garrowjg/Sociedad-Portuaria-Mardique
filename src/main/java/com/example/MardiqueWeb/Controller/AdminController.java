package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.*;
import com.example.MardiqueWeb.Repository.*;
import com.example.MardiqueWeb.Service.*;
import com.example.MardiqueWeb.Config.CustomAuthenticationFailureHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private SiteContentService siteContentService;

    @Autowired
    private FaqRepository faqRepository;

    @Autowired
    private ChatbotRatingRepository chatbotRatingRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private TicketHistorialRepository ticketHistorialRepository;

    @Autowired
    private TicketAdjuntoRepository ticketAdjuntoRepository;

    @Autowired
    private PqrsService pqrsService;

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
        Set<String> blockedUsernames = new java.util.HashSet<>();
        for (User u : userRepository.findAll()) {
            if (CustomAuthenticationFailureHandler.isBlocked(u.getUsername())) {
                blockedUsernames.add(u.getUsername());
            }
        }
        model.addAttribute("blockedUsernames", blockedUsernames);
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

        List<SupportTicket> allTickets = supportTicketRepository.findAll();
        List<SupportTicket> ticketScope = isFullAdmin
            ? allTickets
            : allTickets.stream().filter(t -> dept != null && dept.equals(t.getDepartamento())).toList();
        List<SupportTicket> abiertos = ticketScope.stream()
            .filter(t -> "ABIERTO".equals(t.getStatus()) || "EN_PROCESO".equals(t.getStatus()) || "REQUIERE_INFO".equals(t.getStatus()))
            .toList();
        long pqrsVencidos = abiertos.stream().filter(t -> t.getFechaLimite() != null && t.getFechaLimite().isBefore(LocalDateTime.now())).count();
        long pqrsPorVencer = abiertos.stream().filter(t -> t.getFechaLimite() != null && !t.getFechaLimite().isBefore(LocalDateTime.now())
                && pqrsService.diasRestantesPara(t) <= 2).count();
        long pqrsEnPlazo = abiertos.stream().filter(t -> t.getFechaLimite() != null && !t.getFechaLimite().isBefore(LocalDateTime.now())
                && pqrsService.diasRestantesPara(t) > 2).count();
        long pqrsResueltosHoy = ticketScope.stream()
            .filter(t -> t.getFechaResuelto() != null && t.getFechaResuelto().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
            .count();
        model.addAttribute("pqrsAbiertos", abiertos.size());
        model.addAttribute("pqrsVencidos", pqrsVencidos);
        model.addAttribute("pqrsPorVencer", pqrsPorVencer);
        model.addAttribute("pqrsEnPlazo", pqrsEnPlazo);
        model.addAttribute("pqrsResueltosHoy", pqrsResueltosHoy);
        model.addAttribute("pqrsService", pqrsService);

        model.addAttribute("recentUsers", isFullAdmin
            ? userRepository.findAll().stream().limit(5).toList()
            : List.of());
        return "AdminDashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model, Authentication auth,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser != null && currentUser.getDepartamento() != null) {
            return "redirect:/admin/dashboard";
        }
        Page<User> userPage = userRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", userPage.getTotalPages());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("activeUsers", userRepository.countByEnabled(true));
        model.addAttribute("empresaUsers", userRepository.countByTipo("EMPRESA"));
        model.addAttribute("clienteUsers", userRepository.countByTipo("CLIENTE"));
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

    @PostMapping("/users/unblock/{username}")
    public String unblockUser(@PathVariable String username, HttpServletRequest request, Authentication auth, RedirectAttributes ra) {
        User currentAdmin = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentAdmin != null && currentAdmin.getDepartamento() != null) {
            throw new AccessDeniedException("No tienes permiso para desbloquear usuarios");
        }
        CustomAuthenticationFailureHandler.clearBlock(username);
        auditService.log(auth.getName(), "USER_UNBLOCK", "Unblocked user: " + username, request);
        ra.addFlashAttribute("success", "Bloqueo de " + username + " eliminado");
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
        if (email != null && !email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
            ra.addFlashAttribute("error", "El email ya está registrado en otro usuario");
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
                                   @RequestParam(required = false) String prioridad,
                                   @RequestParam(required = false) String asignado,
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
        List<SupportTicket> pqrsList = new java.util.ArrayList<>();
        List<SupportTicket> chatbotList = new java.util.ArrayList<>();
        for (SupportTicket t : allTickets) {
            if (!isFullAdmin && !(dept != null && dept.equals(t.getDepartamento()))) {
                continue;
            }
            if (!"PQRS".equals(t.getOrigen()) && !"CHATBOT".equals(t.getOrigen())) {
                continue;
            }
            if (estado != null && !estado.isEmpty() && !estado.equals(t.getStatus())) {
                continue;
            }
            if (tipo != null && !tipo.isEmpty() && !tipo.equals(t.getTipoPeticion())) {
                continue;
            }
            if (prioridad != null && !prioridad.isEmpty() && !prioridad.equals(t.getPrioridad())) {
                continue;
            }
            if (asignado != null && !asignado.isEmpty()) {
                if ("SIN_ASIGNAR".equals(asignado) ? t.getAsignadoA() != null : !asignado.equals(t.getAsignadoA())) {
                    continue;
                }
            }
            if (search != null && !search.isEmpty()) {
                String q = search.toLowerCase();
                boolean match = (t.getNombreCompleto() != null && t.getNombreCompleto().toLowerCase().contains(q))
                        || (t.getEmail() != null && t.getEmail().toLowerCase().contains(q))
                        || (t.getRadicado() != null && t.getRadicado().toLowerCase().contains(q))
                        || (t.getNumeroDocumento() != null && t.getNumeroDocumento().toLowerCase().contains(q));
                if (!match) {
                    continue;
                }
            }
            if ("PQRS".equals(t.getOrigen())) {
                pqrsList.add(t);
            } else {
                chatbotList.add(t);
            }
        }

        model.addAttribute("solicitudes", solStream.toList());
        model.addAttribute("pqrsList", pqrsList);
        model.addAttribute("chatbotList", chatbotList);
        model.addAttribute("selEstado", estado);
        model.addAttribute("selTipo", tipo);
        model.addAttribute("selPrioridad", prioridad);
        model.addAttribute("selAsignado", asignado);
        model.addAttribute("search", search);
        model.addAttribute("pqrsService", pqrsService);
        model.addAttribute("adminAdmins", userRepository.findByRole("ROLE_ADMIN"));
        return "AdminSolicitudes";
    }

    @GetMapping("/solicitudes/{id}")
    public String detallePqrs(@PathVariable Long id, Model model, Authentication auth) {
        SupportTicket ticket = supportTicketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        boolean isFullAdmin = currentUser == null || currentUser.getDepartamento() == null;
        if (!isFullAdmin && currentUser != null && !currentUser.getDepartamento().equals(ticket.getDepartamento())) {
            throw new AccessDeniedException("No tienes permiso para ver este ticket");
        }
        model.addAttribute("ticket", ticket);
        model.addAttribute("historial", ticketHistorialRepository.findByTicketIdOrderByFechaAsc(ticket.getId()));
        model.addAttribute("adjuntos", ticketAdjuntoRepository.findByTicketIdOrderByFechaAsc(ticket.getId()));
        model.addAttribute("pqrsService", pqrsService);
        model.addAttribute("adminAdmins", userRepository.findByRole("ROLE_ADMIN"));
        model.addAttribute("isFullAdmin", isFullAdmin);
        return "AdminPqrsDetalle";
    }

    @PostMapping("/solicitudes/{id}/estado")
    @Transactional
    public String cambiarEstadoPqrs(@PathVariable Long id, @RequestParam String estado,
                                     Authentication auth, RedirectAttributes ra) {
        SupportTicket ticket = supportTicketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        String old = ticket.getStatus();
        ticket.setStatus(estado);
        if ("RESUELTO".equals(estado) && ticket.getFechaResuelto() == null) {
            ticket.setFechaResuelto(LocalDateTime.now());
        }
        if ("CERRADO".equals(estado) && ticket.getFechaCerrado() == null) {
            ticket.setFechaCerrado(LocalDateTime.now());
        }
        if ("ABIERTO".equals(estado)) {
            ticket.setFechaResuelto(null);
            ticket.setFechaCerrado(null);
        }
        supportTicketRepository.save(ticket);
        String desc = "Estado cambiado de " + pqrsService.labelEstado(old) + " a " + pqrsService.labelEstado(estado);
        pqrsService.registrarHistorial(ticket.getId(), "ESTADO", desc, auth.getName());
        ra.addFlashAttribute("success", "Estado actualizado a: " + pqrsService.labelEstado(estado));
        return "redirect:/admin/solicitudes/" + id;
    }

    @PostMapping("/solicitudes/{id}/asignar")
    @Transactional
    public String asignarPqrs(@PathVariable Long id, @RequestParam(required = false) String asignadoA,
                              @RequestParam(required = false) String prioridad,
                              @RequestParam(required = false) String departamento,
                              Authentication auth, RedirectAttributes ra) {
        SupportTicket ticket = supportTicketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        String oldAsignado = ticket.getAsignadoA();
        if (asignadoA != null && !asignadoA.isBlank()) {
            ticket.setAsignadoA(asignadoA);
        } else {
            ticket.setAsignadoA(null);
        }
        if (prioridad != null && !prioridad.isBlank()) {
            ticket.setPrioridad(prioridad);
        }
        if (departamento != null && !departamento.isBlank()) {
            ticket.setDepartamento(departamento);
        }
        if ("ABIERTO".equals(ticket.getStatus()) || "REQUIERE_INFO".equals(ticket.getStatus())) {
            ticket.setStatus("EN_PROCESO");
        }
        supportTicketRepository.save(ticket);
        String asignado = ticket.getAsignadoA() != null ? ticket.getAsignadoA() : "sin asignar";
        String desc = "Ticket asignado. Antes: " + (oldAsignado != null ? oldAsignado : "sin asignar")
                + ", ahora: " + asignado + ". Prioridad: " + ticket.getPrioridad();
        pqrsService.registrarHistorial(ticket.getId(), "ASIGNACION", desc, auth.getName());
        ra.addFlashAttribute("success", "Ticket asignado a " + asignado);
        return "redirect:/admin/solicitudes/" + id;
    }

    @PostMapping("/solicitudes/{id}/nota")
    @Transactional
    public String notaInternaPqrs(@PathVariable Long id, @RequestParam String nota,
                                  Authentication auth, RedirectAttributes ra) {
        SupportTicket ticket = supportTicketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        if (nota != null && !nota.isBlank()) {
            pqrsService.registrarHistorial(ticket.getId(), "NOTA_INTERNA", nota.trim(), auth.getName());
            ra.addFlashAttribute("success", "Nota interna registrada");
        } else {
            ra.addFlashAttribute("error", "La nota no puede estar vacía");
        }
        return "redirect:/admin/solicitudes/" + id;
    }

    @PostMapping("/solicitudes/{id}/responder")
    @Transactional
    public String responderPqrs(@PathVariable Long id, @RequestParam String respuesta,
                                @RequestParam(required = false) String cerrar,
                                Authentication auth, RedirectAttributes ra) {
        SupportTicket ticket = supportTicketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        if (respuesta == null || respuesta.isBlank()) {
            ra.addFlashAttribute("error", "La respuesta no puede estar vacía");
            return "redirect:/admin/solicitudes/" + id;
        }
        ticket.setRespuesta(respuesta.trim());
        ticket.setStatus("RESUELTO");
        ticket.setFechaResuelto(LocalDateTime.now());
        if ("on".equals(cerrar)) {
            ticket.setStatus("CERRADO");
            ticket.setFechaCerrado(LocalDateTime.now());
        }
        supportTicketRepository.save(ticket);
        String estadoFinal = "CERRADO".equals(ticket.getStatus()) ? "Cerrado" : "Resuelto";
        pqrsService.registrarHistorial(ticket.getId(), "RESPUESTA",
                "Respuesta enviada al solicitante. Ticket marcado como " + estadoFinal + ".", auth.getName());
        ra.addFlashAttribute("success", "Respuesta enviada. El solicitante puede verla con el radicado " + ticket.getRadicado());
        return "redirect:/admin/solicitudes/" + id;
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
                                @RequestParam(required = false) String search,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        List<Payment> all = paymentRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
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
        List<Payment> filtered = stream.toList();
        int totalItems = filtered.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int from = Math.min(page * size, totalItems);
        int to = Math.min(from + size, totalItems);
        List<Payment> paged = filtered.subList(from, to);

        model.addAttribute("payments", paged);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
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

    @GetMapping("/chatbot")
    public String chatbotAdmin(Model model) {
        List<KnowledgeChunk> allChunks = knowledgeBaseService.findAll();
        Map<String, List<KnowledgeChunk>> grouped = allChunks.stream()
                .filter(chunk -> chunk.getSource() == null || !chunk.getSource().startsWith("Página: "))
                .collect(Collectors.groupingBy(KnowledgeChunk::getSource));
        model.addAttribute("chunks", allChunks.stream()
                .filter(chunk -> chunk.getSource() == null || !chunk.getSource().startsWith("Página: "))
                .collect(Collectors.toList()));
        model.addAttribute("groupedChunks", grouped);
        model.addAttribute("chunkCount", allChunks.size());
        model.addAttribute("sourceCount", grouped.size());
        model.addAttribute("faqs", faqRepository.findAllByOrderByOrdenAsc());
        List<ChatbotRating> ratings = chatbotRatingRepository.findAll(
            Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("ratings", ratings);

        // ── Dashboard / informe del chatbot ─────────────────────────────────────
        long totalMessages = chatMessageRepository.count();
        long userMessages = chatMessageRepository.countByRole("user");
        long botMessages = chatMessageRepository.countByRole("assistant");
        long sessions = chatMessageRepository.countDistinctSessions();

        // Tiempo de respuesta: parea cada mensaje "assistant" con el "user" anterior de la misma sesión.
        // Se recorre solo id/sesión/fecha (sin traer los cuerpos de texto) para no saturar la DB/red.
        List<Object[]> rows = chatMessageRepository.findAllSessionMessages();
        List<Long> responseTimesMs = new ArrayList<>();
        Map<String, LocalDateTime> lastUserBySession = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String sid = (String) row[0];
            String role = (String) row[1];
            Object tsObj = row[2];
            LocalDateTime ts = tsObj instanceof LocalDateTime ? (LocalDateTime) tsObj
                    : tsObj instanceof java.sql.Timestamp ? ((java.sql.Timestamp) tsObj).toLocalDateTime()
                    : null;
            if ("user".equals(role)) {
                lastUserBySession.put(sid, ts);
            } else if ("assistant".equals(role) && lastUserBySession.containsKey(sid)) {
                LocalDateTime userAt = lastUserBySession.remove(sid);
                LocalDateTime botAt = ts;
                if (userAt != null && botAt != null) {
                    long ms = Math.max(0, Duration.between(userAt, botAt).toMillis());
                    responseTimesMs.add(ms);
                }
            }
        }

        // Desglose por tipo de respuesta (faq | form | out_of_bounds | llm_rag | llm)
        long faqCount = chatMessageRepository.countByType("faq");
        long formCount = chatMessageRepository.countByType("form");
        long outOfBoundsCount = chatMessageRepository.countByType("out_of_bounds");
        long llmRagCount = chatMessageRepository.countByType("llm_rag");
        long llmCount = chatMessageRepository.countByType("llm");

        // Votos del usuario por respuesta (up = útil, down = no útil)
        long feedbackUp = chatMessageRepository.countByFeedback("up");
        long feedbackDown = chatMessageRepository.countByFeedback("down");
        long feedbackTotal = feedbackUp + feedbackDown;

        double avgResponseMs = responseTimesMs.stream().mapToLong(Long::longValue).average().orElse(0);
        long minResponseMs = responseTimesMs.stream().mapToLong(Long::longValue).min().orElse(0);
        long maxResponseMs = responseTimesMs.stream().mapToLong(Long::longValue).max().orElse(0);

        java.util.function.Function<Number, String> fmtTime = ms -> String.format(Locale.ROOT, "%.1f", ms.doubleValue() / 1000.0);

        model.addAttribute("chatMessagesCount", totalMessages);
        model.addAttribute("chatUserMessages", userMessages);
        model.addAttribute("chatBotMessages", botMessages);
        model.addAttribute("chatSessions", sessions);
        model.addAttribute("chatTotalAnswered", botMessages);
        model.addAttribute("chatAvgResponse", fmtTime.apply(avgResponseMs));
        model.addAttribute("chatMinResponse", fmtTime.apply(minResponseMs));
        model.addAttribute("chatMaxResponse", fmtTime.apply(maxResponseMs));

        model.addAttribute("chatTypeFaq", faqCount);
        model.addAttribute("chatTypeForm", formCount);
        model.addAttribute("chatTypeOutOfBounds", outOfBoundsCount);
        model.addAttribute("chatTypeLlmRag", llmRagCount);
        model.addAttribute("chatTypeLlm", llmCount);
        model.addAttribute("chatTypeAnswered", botMessages);
        java.util.function.Function<Long, Integer> pctOfTypes = n -> botMessages > 0 ? (int) Math.round(n * 100.0 / botMessages) : 0;
        model.addAttribute("chatTypePctFaq", pctOfTypes.apply(faqCount));
        model.addAttribute("chatTypePctForm", pctOfTypes.apply(formCount));
        model.addAttribute("chatTypePctOutOfBounds", pctOfTypes.apply(outOfBoundsCount));
        model.addAttribute("chatTypePctLlmRag", pctOfTypes.apply(llmRagCount));
        model.addAttribute("chatTypePctLlm", pctOfTypes.apply(llmCount));

        model.addAttribute("chatFeedbackUp", feedbackUp);
        model.addAttribute("chatFeedbackDown", feedbackDown);
        model.addAttribute("chatFeedbackTotal", feedbackTotal);
        model.addAttribute("chatFeedbackPct", feedbackTotal > 0 ? Math.round(feedbackUp * 100.0 / feedbackTotal) : 0);

        // Distribución de valoraciones (reutiliza la lista ya cargada para la vista)
        long[] ratingDist = new long[5];
        for (ChatbotRating r : ratings) {
            if (r.getRating() >= 1 && r.getRating() <= 5) ratingDist[r.getRating() - 1]++;
        }
        long totalRatings = ratings.size();
        long sumRatings = 0;
        for (ChatbotRating r : ratings) sumRatings += r.getRating();
        double avgRating = totalRatings > 0 ? (double) sumRatings / totalRatings : 0;
        model.addAttribute("chatRatingsCount", totalRatings);
        model.addAttribute("chatRatingAvg", String.format(Locale.ROOT, "%.1f", avgRating));
        model.addAttribute("chatRatingDist5", ratingDist[4]);
        model.addAttribute("chatRatingDist4", ratingDist[3]);
        model.addAttribute("chatRatingDist3", ratingDist[2]);
        model.addAttribute("chatRatingDist2", ratingDist[1]);
        model.addAttribute("chatRatingDist1", ratingDist[0]);
        java.util.function.Function<Long, Integer> pctOf = n -> totalRatings > 0 ? (int) Math.round(n * 100.0 / totalRatings) : 0;
        model.addAttribute("chatRatingPct5", pctOf.apply(ratingDist[4]));
        model.addAttribute("chatRatingPct4", pctOf.apply(ratingDist[3]));
        model.addAttribute("chatRatingPct3", pctOf.apply(ratingDist[2]));
        model.addAttribute("chatRatingPct2", pctOf.apply(ratingDist[1]));
        model.addAttribute("chatRatingPct1", pctOf.apply(ratingDist[0]));

        // Solicitudes enviadas desde el chatbot (formulario de contacto del chat)
        long chatContactRequests = supportTicketRepository.countByOrigen("CHATBOT");
        model.addAttribute("chatContactRequests", chatContactRequests);
        return "AdminChatbot";
    }

    @PostMapping("/chatbot/faq/save")
    public String faqSave(@RequestParam(required = false) Long id,
                          @RequestParam String question,
                          @RequestParam String answer,
                          @RequestParam(defaultValue = "0") int orden,
                          @RequestParam(defaultValue = "false") boolean activo,
                          RedirectAttributes ra) {
        if (question == null || question.isBlank() || answer == null || answer.isBlank()) {
            ra.addFlashAttribute("error", "Pregunta y respuesta son obligatorias");
            return "redirect:/admin/chatbot";
        }
        Faq faq;
        if (id != null) {
            faq = faqRepository.findById(id).orElse(null);
            if (faq == null) {
                ra.addFlashAttribute("error", "La pregunta frecuente no existe");
                return "redirect:/admin/chatbot";
            }
        } else {
            faq = new Faq();
        }
        faq.setQuestion(question.trim());
        faq.setAnswer(answer.trim());
        faq.setOrden(orden);
        faq.setActivo(activo);
        faqRepository.save(faq);
        ra.addFlashAttribute("success", id != null ? "Pregunta frecuente actualizada" : "Pregunta frecuente agregada");
        return "redirect:/admin/chatbot";
    }

    @PostMapping("/chatbot/faq/delete/{id}")
    public String faqDelete(@PathVariable Long id, RedirectAttributes ra) {
        faqRepository.deleteById(id);
        ra.addFlashAttribute("success", "Pregunta frecuente eliminada");
        return "redirect:/admin/chatbot";
    }

    @PostMapping("/chatbot/upload")
    public String chatbotUpload(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Selecciona un archivo PDF");
            return "redirect:/admin/chatbot";
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            ra.addFlashAttribute("error", "Solo se permiten archivos PDF");
            return "redirect:/admin/chatbot";
        }
        try {
            int chunks = knowledgeBaseService.processPdf(file, filename);
            ra.addFlashAttribute("success", "PDF procesado: " + chunks + " fragmentos generados desde \"" + filename + "\"");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al procesar el PDF: " + e.getMessage());
        }
        return "redirect:/admin/chatbot";
    }

    @PostMapping("/chatbot/text")
    public String chatbotText(@RequestParam String title, @RequestParam String content, RedirectAttributes ra) {
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            ra.addFlashAttribute("error", "Completa todos los campos");
            return "redirect:/admin/chatbot";
        }
        try {
            int chunks = knowledgeBaseService.processRawText(content, title);
            ra.addFlashAttribute("success", "Texto agregado: " + chunks + " fragmentos generados desde \"" + title + "\"");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al procesar el texto: " + e.getMessage());
        }
        return "redirect:/admin/chatbot";
    }

    @PostMapping("/chatbot/delete")
    public String chatbotDelete(@RequestParam String source, RedirectAttributes ra) {
        knowledgeBaseService.deleteBySource(source);
        ra.addFlashAttribute("success", "Documento \"" + source + "\" eliminado de la base de conocimiento");
        return "redirect:/admin/chatbot";
    }

    @PostMapping("/chatbot/index-pages")
    public String chatbotIndexPages(RedirectAttributes ra) {
        try {
            int chunks = siteContentService.indexAllPages();
            ra.addFlashAttribute("success", "Páginas del sitio web indexadas: " + chunks + " fragmentos para el chatbot");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al indexar las páginas del sitio: " + e.getMessage());
        }
        return "redirect:/admin/chatbot";
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
        List<SupportTicket> mensajes = supportTicketRepository.findByOrigenOrderByCreatedAtDesc("CONTACTO");
        model.addAttribute("mensajes", mensajes);
        model.addAttribute("recibidos", mensajes.size());
        model.addAttribute("respondidos", mensajes.stream().filter(m -> "RESUELTO".equals(m.getStatus())).count());
        model.addAttribute("pendientes", mensajes.stream()
                .filter(m -> "ABIERTO".equals(m.getStatus()) || "EN_PROCESO".equals(m.getStatus())).count());
        return "AdminMensajes";
    }

    @PostMapping("/mensajes/{id}/estado")
    @Transactional
    public String cambiarEstadoMensaje(@PathVariable Long id, @RequestParam String estado,
                                       Authentication auth, RedirectAttributes ra) {
        SupportTicket ticket = supportTicketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));
        if (!"CONTACTO".equals(ticket.getOrigen())) {
            throw new RuntimeException("No es un mensaje de contacto");
        }
        ticket.setStatus(estado);
        if ("RESUELTO".equals(estado) && ticket.getFechaResuelto() == null) {
            ticket.setFechaResuelto(LocalDateTime.now());
        }
        if ("ABIERTO".equals(estado)) {
            ticket.setFechaResuelto(null);
        }
        supportTicketRepository.save(ticket);
        pqrsService.registrarHistorial(ticket.getId(), "ESTADO",
                "Estado del mensaje cambiado a " + pqrsService.labelEstado(estado), auth.getName());
        ra.addFlashAttribute("success", "Estado del mensaje actualizado a: " + pqrsService.labelEstado(estado));
        return "redirect:/admin/mensajes";
    }

    @PostMapping("/mensajes/{id}/eliminar")
    @Transactional
    public String eliminarMensaje(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        SupportTicket ticket = supportTicketRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));
        if (!"CONTACTO".equals(ticket.getOrigen())) {
            throw new RuntimeException("No es un mensaje de contacto");
        }
        supportTicketRepository.delete(ticket);
        ra.addFlashAttribute("success", "Mensaje eliminado correctamente");
        return "redirect:/admin/mensajes";
    }
}
