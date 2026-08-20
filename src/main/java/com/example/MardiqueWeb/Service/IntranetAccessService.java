package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.IntranetAccessRole;
import com.example.MardiqueWeb.Entity.IntranetDocument;
import com.example.MardiqueWeb.Entity.IntranetDocumentView;
import com.example.MardiqueWeb.Repository.IntranetAccessRoleRepository;
import com.example.MardiqueWeb.Repository.IntranetDocumentRepository;
import com.example.MardiqueWeb.Repository.IntranetDocumentViewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IntranetAccessService {

    private final IntranetDocumentViewRepository viewRepository;
    private final IntranetAccessRoleRepository roleRepository;
    private final IntranetDocumentRepository documentRepository;

    // En versión de pruebas los permisos quedan abiertos para poder demostrar la
    // funcionalidad; en producción se debe activar la validación de roles.
    @Value("${intranet.test-mode:true}")
    private boolean testMode;

    public IntranetAccessService(IntranetDocumentViewRepository viewRepository,
                                 IntranetAccessRoleRepository roleRepository,
                                 IntranetDocumentRepository documentRepository) {
        this.viewRepository = viewRepository;
        this.roleRepository = roleRepository;
        this.documentRepository = documentRepository;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public boolean isAdmin(String email) {
        if (email == null || email.isBlank()) return false;
        if (testMode) return true;
        return roleRepository.existsByEmailAndRole(email.trim().toLowerCase(), IntranetAccessRole.ROLE_ADMIN);
    }

    public boolean canViewViewers(String email) {
        if (email == null || email.isBlank()) return false;
        if (testMode) return true;
        String e = email.trim().toLowerCase();
        return roleRepository.existsByEmailAndRole(e, IntranetAccessRole.ROLE_ADMIN)
                || roleRepository.existsByEmailAndRole(e, IntranetAccessRole.ROLE_VIEWER);
    }

    /** Registra una vista de un documento. */
    public boolean recordView(Long documentId, String email, String name) {
        if (documentId == null || email == null || email.isBlank()) return false;
        IntranetDocumentView view = new IntranetDocumentView();
        view.setDocumentId(documentId);
        view.setViewerEmail(email.trim().toLowerCase());
        view.setViewerName(name == null || name.isBlank() ? null : name.trim());
        view.setViewedAt(LocalDateTime.now());
        viewRepository.save(view);
        return true;
    }

    /**
     * Resumen de quién ha visto un documento, agrupado por correo:
     * { email, name, veces, primeraVez, ultimaVez }
     */
    public List<Map<String, Object>> viewsByDocument(Long documentId) {
        List<IntranetDocumentView> rows = viewRepository.findByDocumentIdOrderByViewedAtAsc(documentId);
        Map<String, Map<String, Object>> byEmail = new LinkedHashMap<>();
        for (IntranetDocumentView v : rows) {
            Map<String, Object> agg = byEmail.computeIfAbsent(v.getViewerEmail(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("email", v.getViewerEmail());
                m.put("name", v.getViewerName() == null ? "" : v.getViewerName());
                m.put("veces", 0);
                return m;
            });
            agg.put("veces", ((Number) agg.get("veces")).intValue() + 1);
            if (!agg.containsKey("primeraVez")) agg.put("primeraVez", v.getViewedAt().toString());
            agg.put("ultimaVez", v.getViewedAt().toString());
        }
        return new ArrayList<>(byEmail.values());
    }

    /** Todos los eventos de vista (para el panel de administración). */
    public List<Map<String, Object>> allViewEvents() {
        List<IntranetDocumentView> rows = viewRepository.findAllByOrderByViewedAtDesc();
        List<Map<String, Object>> events = new ArrayList<>();
        for (IntranetDocumentView v : rows) {
            IntranetDocument doc = v.getDocumentId() == null ? null : documentRepository.findById(v.getDocumentId()).orElse(null);
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("documentId", v.getDocumentId());
            e.put("documentName", doc != null ? doc.getNombre() : "Documento eliminado");
            e.put("sector", doc != null ? doc.getSector() : "");
            e.put("email", v.getViewerEmail());
            e.put("name", v.getViewerName() == null ? "" : v.getViewerName());
            e.put("viewedAt", v.getViewedAt().toString());
            events.add(e);
        }
        return events;
    }

    /** Conteo total de vistas registradas. */
    public long totalViews() {
        return viewRepository.count();
    }

    /** Total de documentos subidos. */
    public long totalDocuments() {
        return documentRepository.count();
    }

    public List<IntranetAccessRole> listByRole(String role) {
        return roleRepository.findByRole(role);
    }

    public boolean grantRole(String email, String role) {
        if (email == null || email.isBlank() || role == null || role.isBlank()) return false;
        String e = email.trim().toLowerCase();
        if (roleRepository.findByEmail(e).isPresent()) {
            return roleRepository.existsByEmailAndRole(e, role);
        }
        roleRepository.save(new IntranetAccessRole(e, role));
        return true;
    }

    public void revokeRole(String email) {
        if (email == null || email.isBlank()) return;
        roleRepository.deleteByEmail(email.trim().toLowerCase());
    }
}