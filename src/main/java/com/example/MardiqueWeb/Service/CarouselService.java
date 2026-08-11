package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.CarouselEntry;
import com.example.MardiqueWeb.Repository.CarouselEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CarouselService {

    @Autowired
    private CarouselEntryRepository carouselEntryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, List<Object[]>> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("inicio", List.of(
                new Object[]{"Puerto1.png", "Carga y Descarga de Precisión", "Maniobras ágiles y seguras de mercancías a granel y sacos de gran formato directamente hacia el transporte terrestre."},
                new Object[]{"Puerto2.png", "Capacidad a Gran Escala", "Infraestructura robusta diseñada para el atraque de buques de gran calado, respaldada por asistencia especializada de remolcadores."},
                new Object[]{"Puerto3.png", "Conectividad Multimodal", "Plataforma operativa y muelles estratégicos optimizados para la transferencia eficiente de mercancías por vías fluviales y marítimas."},
                new Object[]{"Puerto4.png", "Infraestructura de Vanguardia", "Equipamiento de última generación, zonas de almacenamiento y personal altamente calificado para el control y despacho seguro de la carga."}
        ));
        DEFAULTS.put("servicios", List.of(
                new Object[]{"MuelleMaritimo.jpg", "Muelle Marítimo", "Operaciones de muellaje y atraque de embarcaciones respaldadas por infraestructura robusta y tecnología de precisión para garantizar la eficiencia en cada maniobra."},
                new Object[]{"MuelleFluvial.jpg", "Muelle Fluvial", "Conexión estratégica que permite operaciones de muellaje y atraque simultáneo de convoy, remolcadores y embarcaciones marítimas y fluviales."},
                new Object[]{"ServiciosPortuarios.jpg", "Servicios Portuarios", "Personal altamente calificado, equipos de última tecnología e infraestructura para el manejo integral de carga, almacenamiento y control de inventarios."},
                new Object[]{"PlataformaLogistica.jpg", "Plataforma Logística", "Hub regional con bodegas, patios de maniobras y conexiones estratégicas para la distribución eficiente de mercancías a nivel nacional e internacional."}
        ));
    }

    @PostConstruct
    public void ensureDefaults() {
        for (String section : DEFAULTS.keySet()) {
            if (carouselEntryRepository.findBySectionOrderByIdAsc(section).isEmpty()) {
                for (Object[] d : DEFAULTS.get(section)) {
                    CarouselEntry e = new CarouselEntry(section,
                            (String) d[0], null, (String) d[1], (String) d[2]);
                    carouselEntryRepository.save(e);
                }
            }
        }
    }

    public List<CarouselEntry> findBySection(String section) {
        return carouselEntryRepository.findBySectionOrderByIdAsc(section);
    }

    public Map<String, Object> dataForSection(String section) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (CarouselEntry e : findBySection(section)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", e.getTitulo());
            item.put("desc", e.getDescripcion());
            item.put("url", e.getFilePath() != null && !e.getFilePath().isEmpty()
                    ? e.getFilePath() : "/images/" + e.getImageKey());
            result.put(e.getImageKey(), item);
        }
        return result;
    }

    public Map<String, Object> getCarouselData() {
        Map<String, Object> sets = new LinkedHashMap<>();
        sets.put("inicio", dataForSection("inicio"));
        sets.put("servicios", dataForSection("servicios"));
        return sets;
    }

    public String buildCarouselJson() {
        try {
            return objectMapper.writeValueAsString(getCarouselData());
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Restaura la entrada a sus valores por defecto: imagen original local
     * (/images/clave) y el título/descripción del catálogo base.
     * @return true si la entrada existía y tenía un valor por defecto.
     */
    public boolean resetToDefault(Long id) {
        CarouselEntry entry = carouselEntryRepository.findById(id).orElse(null);
        if (entry == null) return false;
        List<Object[]> defaults = DEFAULTS.get(entry.getSection());
        if (defaults == null) return false;
        for (Object[] d : defaults) {
            if (d[0].equals(entry.getImageKey())) {
                entry.setTitulo((String) d[1]);
                entry.setDescripcion((String) d[2]);
                entry.setFilePath(null);
                carouselEntryRepository.save(entry);
                return true;
            }
        }
        return false;
    }
}