package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_adjuntos")
public class TicketAdjunto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    @Column(name = "url")
    private String url;

    @Column(name = "origen")
    private String origen;

    @Column(name = "fecha")
    private LocalDateTime fecha = LocalDateTime.now();

    public TicketAdjunto() {}

    public TicketAdjunto(Long ticketId, String nombreArchivo, String url, String origen) {
        this.ticketId = ticketId;
        this.nombreArchivo = nombreArchivo;
        this.url = url;
        this.origen = origen;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}