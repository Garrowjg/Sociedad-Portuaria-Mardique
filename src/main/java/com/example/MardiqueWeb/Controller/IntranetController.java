package com.example.MardiqueWeb.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/intranet")
public class IntranetController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "IntranetDashboard";
    }

    @GetMapping("/documentos")
    public String documentos() {
        return "IntranetDocumentos";
    }

    @GetMapping("/noticias")
    public String noticias() {
        return "IntranetNoticias";
    }

    @GetMapping("/areas")
    public String areas() {
        return "IntranetAreas";
    }

    @GetMapping("/galeria")
    public String galeria() {
        return "IntranetGaleria";
    }

    @GetMapping("/calendario")
    public String calendario() {
        return "IntranetCalendario";
    }

    @GetMapping("/conversaciones")
    public String conversaciones() {
        return "IntranetConversaciones";
    }

    @GetMapping("/soporte")
    public String soporte() {
        return "IntranetSoporte";
    }

    @GetMapping("/informes")
    public String informes() {
        return "IntranetInformes";
    }
}
