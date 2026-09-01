package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.SolicitudHr;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SolicitudHrRepository extends JpaRepository<SolicitudHr, Long> {
    List<SolicitudHr> findBySenderEmailOrderByCreatedAtDesc(String senderEmail);
    List<SolicitudHr> findByStatusOrderByCreatedAtDesc(String status);
    List<SolicitudHr> findAllByOrderByCreatedAtDesc();
}
