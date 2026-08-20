package com.example.MardiqueWeb.Controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * Genera imágenes placeholder SVG ligeras y locales (sin depender de CDNs
 * externos como picsum.photos o freepik que ralentizan la carga en modo prueba).
 */
@RestController
public class PlaceholderController {

    private static final String[] PALETTE = {
        "#1a73e8", "#0d47a1",
        "#e8710a", "#b45309",
        "#0d9488", "#115e59",
        "#7c3aed", "#5b21b6",
        "#dc2626", "#991b1b",
        "#16a34a", "#14532d",
        "#0ea5e9", "#075985",
        "#d946ef", "#86198f"
    };

    private static int hash(String seed) {
        int h = 0;
        for (char c : seed.toCharArray()) {
            h = (h * 31 + c) & 0x7fffffff;
        }
        return h;
    }

    @GetMapping(value = "/api/placeholder/{seed}", produces = "image/svg+xml")
    public ResponseEntity<byte[]> placeholder(@PathVariable String seed) {
        return build(seed, 600, 400);
    }

    @GetMapping(value = "/api/placeholder/{seed}/{w}/{h}", produces = "image/svg+xml")
    public ResponseEntity<byte[]> placeholder(@PathVariable String seed,
                                              @PathVariable int w,
                                              @PathVariable int h) {
        return build(seed, w, h);
    }

    private ResponseEntity<byte[]> build(String seed, int w, int h) {
        int hh = hash(seed);
        String c1 = PALETTE[hh % PALETTE.length];
        String c2 = PALETTE[(hh / PALETTE.length) % PALETTE.length];
        String label = seed.replaceAll("_", " ").replaceAll("-", " ")
                .replaceAll("(?<=\\w)(\\d+)", " $1")
                .trim();
        if (label.length() > 24) {
            label = label.substring(0, 24);
        }
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"" + w + "\" height=\"" + h + "\" viewBox=\"0 0 " + w + " " + h + "\">"
                + "<defs><linearGradient id=\"g\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">"
                + "<stop offset=\"0\" stop-color=\"" + c1 + "\"/>"
                + "<stop offset=\"1\" stop-color=\"" + c2 + "\"/>"
                + "</linearGradient></defs>"
                + "<rect width=\"" + w + "\" height=\"" + h + "\" fill=\"url(#g)\"/>"
                + "<circle cx=\"" + (w * 0.8) + "\" cy=\"" + (h * 0.2) + "\" r=\"" + (h * 0.3) + "\" fill=\"rgba(255,255,255,0.12)\"/>"
                + "<circle cx=\"" + (w * 0.15) + "\" cy=\"" + (h * 0.85) + "\" r=\"" + (h * 0.25) + "\" fill=\"rgba(255,255,255,0.10)\"/>"
                + "<text x=\"" + (w / 2) + "\" y=\"" + (h / 2) + "\" text-anchor=\"middle\" dominant-baseline=\"middle\" "
                + "font-family=\"Arial, sans-serif\" font-size=\"" + Math.max(14, Math.min(48, w / 18)) + "\" font-weight=\"bold\" "
                + "fill=\"rgba(255,255,255,0.9)\">" + escape(label) + "</text>"
                + "</svg>";
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=86400")
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svg.getBytes(StandardCharsets.UTF_8));
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}