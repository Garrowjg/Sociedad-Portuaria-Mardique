package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.SolicitudHrRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SolicitudHrRecipientRepository extends JpaRepository<SolicitudHrRecipient, Long> {
    List<SolicitudHrRecipient> findBySolicitudIdOrderByCreatedAtDesc(Long solicitudId);
    List<SolicitudHrRecipient> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);
    List<SolicitudHrRecipient> findBySolicitudIdAndRecipientEmail(Long solicitudId, String recipientEmail);

    @Query("SELECT r FROM SolicitudHrRecipient r WHERE r.solicitudId IN :ids ORDER BY r.createdAt DESC")
    List<SolicitudHrRecipient> findBySolicitudIds(@Param("ids") List<Long> ids);
}
