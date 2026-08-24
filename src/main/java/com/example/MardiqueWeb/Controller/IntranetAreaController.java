package com.example.MardiqueWeb.Controller;

import com.example.MardiqueWeb.Entity.IntranetArea;
import com.example.MardiqueWeb.Repository.IntranetAreaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/intranet/areas")
public class IntranetAreaController {

    private final IntranetAreaRepository repo;

    public IntranetAreaController(IntranetAreaRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public java.util.List<IntranetArea> list() {
        return repo.findAllByOrderByNombreAsc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        IntranetArea area = new IntranetArea();
        area.setNombre((String) body.getOrDefault("nombre", ""));
        area.setDescripcion((String) body.getOrDefault("descripcion", ""));
        area.setContactos((String) body.getOrDefault("contactos", ""));
        area.setInforme((String) body.getOrDefault("informe", ""));
        area.setCover((String) body.getOrDefault("cover", ""));
        area.setSitio((String) body.getOrDefault("sitio", ""));
        repo.save(area);
        return ResponseEntity.ok(area);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        IntranetArea area = repo.findById(id).orElse(null);
        if (area == null) return ResponseEntity.notFound().build();
        if (body.containsKey("nombre")) area.setNombre((String) body.get("nombre"));
        if (body.containsKey("descripcion")) area.setDescripcion((String) body.get("descripcion"));
        if (body.containsKey("contactos")) area.setContactos((String) body.get("contactos"));
        if (body.containsKey("informe")) area.setInforme((String) body.get("informe"));
        if (body.containsKey("cover")) area.setCover((String) body.get("cover"));
        if (body.containsKey("sitio")) area.setSitio((String) body.get("sitio"));
        repo.save(area);
        return ResponseEntity.ok(area);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
