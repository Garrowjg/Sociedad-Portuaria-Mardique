package com.example.MardiqueWeb.Service;

import com.example.MardiqueWeb.Entity.PageMedia;
import com.example.MardiqueWeb.Repository.PageMediaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PageMediaService {

    @Autowired
    private PageMediaRepository pageMediaRepository;

    private record MediaSeed(String key, String label, int orden, String path, String type) {}

    private static final Map<String, List<MediaSeed>> DEFAULTS = new LinkedHashMap<>();
    static {
        DEFAULTS.put("inicio", List.of(
                new MediaSeed("video", "Video corporativo (cabecera)", 1, "/videos/VideoCorporativo.mp4", "VIDEO"),
                new MediaSeed("cta", "Fondo franja final — Operar", 2, "/images/Operar.png", "IMAGE")
        ));
        DEFAULTS.put("empresa", List.of(
                new MediaSeed("hero", "Imagen de cabecera — Equipo Mardique", 1, "/images/EquipoMardique.png", "IMAGE"),
                new MediaSeed("vision", "Fondo de Misión y Visión", 2, "/images/VisionMision.png", "IMAGE"),
                new MediaSeed("intro", "Imagen intro — Operador Portuario", 3, "/images/OperadorPortuario.png", "IMAGE"),
                new MediaSeed("cta", "Fondo franja final — Operar", 4, "/images/Operar.png", "IMAGE")
        ));
        DEFAULTS.put("servicios", List.of(
                new MediaSeed("hero", "Imagen de cabecera", 1, "/images/Servicios.png", "IMAGE"),
                new MediaSeed("cta", "Fondo franja final — Operar", 2, "/images/Operar.png", "IMAGE")
        ));
        DEFAULTS.put("galeria", List.of(
                new MediaSeed("hero", "Imagen de cabecera", 1, "https://spmardique.com/wp-content/uploads/2021/12/Editadas_0003_DJI_0098.jpg", "IMAGE"),
                new MediaSeed("cta", "Fondo franja final — Operar", 2, "/images/Operar.png", "IMAGE")
        ));
        DEFAULTS.put("contacto", List.of(
                new MediaSeed("hero", "Imagen de cabecera", 1, "/images/Contacto.png", "IMAGE"),
                new MediaSeed("cta", "Fondo franja final — Operar", 2, "/images/Operar.png", "IMAGE")
        ));
        DEFAULTS.put("tarifas", List.of(
                new MediaSeed("hero", "Imagen de cabecera", 1, "https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=1600&q=80", "IMAGE"),
                new MediaSeed("cta", "Fondo franja final — Operar", 2, "/images/Operar.png", "IMAGE")
        ));
        DEFAULTS.put("procedimientos", List.of(
                new MediaSeed("hero", "Imagen de cabecera", 1, "https://images.unsplash.com/photo-1600880292203-757bb62b4baf?auto=format&fit=crop&w=1600&q=80", "IMAGE"),
                new MediaSeed("cta", "Fondo franja final — Operar", 2, "/images/Operar.png", "IMAGE")
        ));
        DEFAULTS.put("tramites", List.of(
                new MediaSeed("hero", "Imagen de cabecera", 1, "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?auto=format&fit=crop&w=1600&q=80", "IMAGE"),
                new MediaSeed("cta", "Fondo franja final — Operar", 2, "/images/Operar.png", "IMAGE")
        ));
    }

    public static final List<String> PAGE_ORDER = List.of("inicio", "empresa", "servicios", "galeria", "contacto", "tarifas", "procedimientos", "tramites");
    public static final Map<String, String> PAGE_LABELS = Map.of(
            "inicio", "Página Inicio",
            "empresa", "Página Empresa",
            "servicios", "Página Servicios",
            "galeria", "Página Galería",
            "contacto", "Página Contacto",
            "tarifas", "Página Tarifas",
            "procedimientos", "Página Procedimientos",
            "tramites", "Página Trámites en Línea"
    );

    @PostConstruct
    public void ensureDefaults() {
        for (Map.Entry<String, List<MediaSeed>> e : DEFAULTS.entrySet()) {
            Set<String> existingKeys = pageMediaRepository.findByPageOrderByIdAsc(e.getKey())
                    .stream()
                    .map(PageMedia::getMediaKey)
                    .collect(Collectors.toSet());
            for (MediaSeed s : e.getValue()) {
                if (!existingKeys.contains(s.key())) {
                    pageMediaRepository.save(new PageMedia(e.getKey(), s.key(), s.label(), s.orden(), s.path(), s.type()));
                }
            }
        }
    }

    public String resolvedUrl(String page, String key) {
        Optional<PageMedia> media = pageMediaRepository.findByPageAndMediaKey(page, key);
        return media.map(PageMedia::getDisplayUrl).orElse("");
    }

    public List<PageMedia> forPage(String page) {
        return pageMediaRepository.findByPageOrderByIdAsc(page);
    }

    public Map<String, List<PageMedia>> groupByPage() {
        Map<String, List<PageMedia>> map = new LinkedHashMap<>();
        for (String page : PAGE_ORDER) {
            List<PageMedia> list = pageMediaRepository.findByPageOrderByIdAsc(page);
            if (!list.isEmpty()) map.put(page, list);
        }
        return map;
    }
}