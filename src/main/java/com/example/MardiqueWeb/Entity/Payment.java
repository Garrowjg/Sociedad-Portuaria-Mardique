package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private BigDecimal monto;
    private String referencia;
    private String concepto;
    private String moneda = "COP";

    private String email;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "comprobante_usuario")
    private String comprobanteUsuario;

    @Column(name = "comprobante_path")
    private String comprobantePath;

    @Column(name = "comprobante_manual_path")
    private String comprobanteManualPath;

    private boolean processed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Payment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }
    public String getComprobanteUsuario() { return comprobanteUsuario; }
    public void setComprobanteUsuario(String comprobanteUsuario) { this.comprobanteUsuario = comprobanteUsuario; }
    public String getComprobantePath() { return comprobantePath; }
    public void setComprobantePath(String comprobantePath) { this.comprobantePath = comprobantePath; }
    public String getComprobanteManualPath() { return comprobanteManualPath; }
    public void setComprobanteManualPath(String comprobanteManualPath) { this.comprobanteManualPath = comprobanteManualPath; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
