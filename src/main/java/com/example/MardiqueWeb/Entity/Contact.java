package com.example.MardiqueWeb.Entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "contacts")
public class Contact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String cargo;
    private String telefono;
    private String email;
    private String email2;
    private String area;
    private String icono;
    @Column(columnDefinition = "TEXT")
    private String notas;
    private LocalDate createdAt = LocalDate.now();

    public Contact() {}

    public Contact(String nombre, String cargo, String telefono, String email, String email2, String area, String notas) {
        this.nombre = nombre;
        this.cargo = cargo;
        this.telefono = telefono;
        this.email = email;
        this.email2 = email2;
        this.area = area;
        this.notas = notas;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmail2() { return email2; }
    public void setEmail2(String email2) { this.email2 = email2; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate createdAt) { this.createdAt = createdAt; }
}
