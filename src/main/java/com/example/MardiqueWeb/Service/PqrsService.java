package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.SupportTicket;
import com.example.MardiqueWeb.Entity.TicketAdjunto;
import com.example.MardiqueWeb.Entity.TicketHistorial;
import com.example.MardiqueWeb.Repository.SupportTicketRepository;
import com.example.MardiqueWeb.Repository.TicketAdjuntoRepository;
import com.example.MardiqueWeb.Repository.TicketHistorialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PqrsService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private TicketHistorialRepository ticketHistorialRepository;

    @Autowired
    private TicketAdjuntoRepository ticketAdjuntoRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    public static final Map<String, Integer> SLA_DIAS_HABILES = new LinkedHashMap<>() {{
        put("Petición", 5);
        put("Solicitud", 10);
        put("Queja", 15);
        put("Reclamo", 15);
    }};

    public static final Map<String, String> PRIORIDAD_POR_TIPO = new LinkedHashMap<>() {{
        put("Petición", "MEDIA");
        put("Solicitud", "MEDIA");
        put("Queja", "ALTA");
        put("Reclamo", "ALTA");
    }};

    public static final Map<String, String> ESTADO_LABEL = new LinkedHashMap<>() {{
        put("ABIERTO", "Recibido / Pendiente");
        put("EN_PROCESO", "En revisión / Asignado");
        put("REQUIERE_INFO", "Requerimiento de información");
        put("RESUELTO", "Resuelto / Contestado");
        put("CERRADO", "Cerrado");
    }};

    public static String labelEstado(String estado) {
        return ESTADO_LABEL.getOrDefault(estado, estado);
    }

    public static int slaDiasPara(String tipoPeticion) {
        Integer dias = SLA_DIAS_HABILES.get(tipoPeticion);
        return dias != null ? dias : 10;
    }

    public static String prioridadPara(String tipoPeticion) {
        String p = PRIORIDAD_POR_TIPO.get(tipoPeticion);
        return p != null ? p : "MEDIA";
    }

    public static LocalDateTime calcularFechaLimite(String tipoPeticion, LocalDateTime desde) {
        int diasHabiles = slaDiasPara(tipoPeticion);
        LocalDate fecha = desde.toLocalDate();
        int contados = 0;
        while (contados < diasHabiles) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                contados++;
            }
        }
        return fecha.atTime(23, 59, 59);
    }

    public String generarRadicado() {
        String anio = String.valueOf(LocalDate.now().getYear());
        long n = supportTicketRepository.countByRadicadoPrefix("PQRS-" + anio);
        return String.format("PQRS-%s-%04d", anio, n + 1);
    }

    @Transactional
    public void registrarHistorial(Long ticketId, String accion, String descripcion, String usuario) {
        ticketHistorialRepository.save(new TicketHistorial(ticketId, accion, descripcion, usuario));
    }

    @Transactional
    public SupportTicket radicarTicket(SupportTicket ticket) {
        ticket.setRadicado(generarRadicado());
        ticket.setPrioridad(prioridadPara(ticket.getTipoPeticion()));
        ticket.setFechaLimite(calcularFechaLimite(ticket.getTipoPeticion(), LocalDateTime.now()));
        SupportTicket saved = supportTicketRepository.save(ticket);
        registrarHistorial(saved.getId(), "RADICADO",
                "PQRS recibida con radicado " + saved.getRadicado() + ". Plazo de respuesta: " +
                        slaDiasPara(ticket.getTipoPeticion()) + " días hábiles.",
                ticket.getNombreCompleto() != null ? ticket.getNombreCompleto() : "Sistema");
        return saved;
    }

    @Transactional
    public List<TicketAdjunto> guardarAdjuntos(Long ticketId, List<MultipartFile> archivos, String origen) {
        List<TicketAdjunto> guardados = new ArrayList<>();
        if (archivos == null) {
            return guardados;
        }
        for (MultipartFile f : archivos) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            try {
                String url = cloudinaryService.uploadFile(f);
                TicketAdjunto adj = new TicketAdjunto(ticketId, f.getOriginalFilename(), url, origen);
                guardados.add(ticketAdjuntoRepository.save(adj));
            } catch (IOException e) {
                registrarHistorial(ticketId, "ADJUNTO_ERROR",
                        "No se pudo subir el archivo " + (f.getOriginalFilename() != null ? f.getOriginalFilename() : "sin nombre") + ": " + e.getMessage(),
                        "Sistema");
            }
        }
        return guardados;
    }

    public String formatearFecha(LocalDateTime fecha) {
        if (fecha == null) {
            return "-";
        }
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public static boolean isTerminal(String status) {
        return "RESUELTO".equals(status) || "CERRADO".equals(status);
    }

    public long diasRestantesPara(SupportTicket ticket) {
        if (ticket.getFechaLimite() == null || isTerminal(ticket.getStatus())) {
            return Long.MAX_VALUE;
        }
        return java.time.Duration.between(LocalDate.now().atStartOfDay(), ticket.getFechaLimite().toLocalDate().atStartOfDay()).toDays();
    }

    public String slaBadgeClass(SupportTicket ticket) {
        if (ticket.getFechaLimite() == null || isTerminal(ticket.getStatus())) {
            return "";
        }
        long dias = diasRestantesPara(ticket);
        if (dias < 0) {
            return "sla-vencido";
        }
        if (dias <= 2) {
            return "sla-porvencer";
        }
        return "sla-enplazo";
    }

    public String slaBadgeText(SupportTicket ticket) {
        if (isTerminal(ticket.getStatus())) {
            return "Finalizado";
        }
        if (ticket.getFechaLimite() == null) {
            return "Sin plazo";
        }
        long dias = diasRestantesPara(ticket);
        if (dias < 0) {
            return "Vencido por " + Math.abs(dias) + " día(s)";
        }
        if (dias == 0) {
            return "Vence hoy";
        }
        return "Vence en " + dias + " día(s)";
    }
}