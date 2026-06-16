package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByUsername(String username);
    long countByUsername(String username);
    long countByEstado(String estado);
    List<Solicitud> findByDepartamento(String departamento);
    long countByDepartamento(String departamento);
}
