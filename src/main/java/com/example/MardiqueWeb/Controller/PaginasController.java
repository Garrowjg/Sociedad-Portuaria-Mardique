package com.example.MardiqueWeb.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginasController {

    @GetMapping({"/", "/inicio"})
    public String inicio() {
        return "Inicio";
    }

    @GetMapping("/empresa")
    public String empresa() {
        return "Empresa";
    }

    @GetMapping("/servicios")
    public String servicios() {
        return "Servicios";
    }

    @GetMapping("/procedimientos")
    public String procedimientos() {
        return "Procedimientos";
    }

    @GetMapping("/tarifas")
    public String tarifas() {
        return "Tarifas";
    }

    @GetMapping("/tramites-en-linea")
    public String tramitesEnLinea() {
        return "Tramitesenlinea";
    }

    @GetMapping("/galeria")
    public String galeria() {
        return "Galeria";
    }

    @GetMapping("/contacto")
    public String contacto() {
        return "Contacto";
    }
}