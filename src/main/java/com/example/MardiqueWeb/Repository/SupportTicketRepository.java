package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByStatus(String status);
    List<SupportTicket> findByUsername(String username);
    long countByStatus(String status);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    List<SupportTicket> findByOrigenOrderByCreatedAtDesc(String origen);
    long countByOrigen(String origen);
    Optional<SupportTicket> findByRadicado(String radicado);
    long countByStatusAndOrigen(String status, String origen);

    @Query("select count(t) from SupportTicket t where t.radicado like concat(:prefix, '%')")
    long countByRadicadoPrefix(@Param("prefix") String prefix);

    @Query("select t from SupportTicket t where t.status in ('ABIERTO','EN_PROCESO','REQUIERE_INFO') and t.fechaLimite is not null and t.fechaLimite < :now")
    List<SupportTicket> findVencidos(@Param("now") LocalDateTime now);

    @Query("select t from SupportTicket t where t.status in ('ABIERTO','EN_PROCESO','REQUIERE_INFO') and t.fechaLimite is not null and t.fechaLimite >= :now")
    List<SupportTicket> findEnPlazo(@Param("now") LocalDateTime now);

    @Query("select t from SupportTicket t where t.status in ('ABIERTO','EN_PROCESO','REQUIERE_INFO') and t.fechaLimite is not null and t.fechaLimite between :now and :limite")
    List<SupportTicket> findPorVencer(@Param("now") LocalDateTime now, @Param("limite") LocalDateTime limite);

    @Query("select t from SupportTicket t where t.fechaResuelto is not null")
    List<SupportTicket> findAllResueltos();

    @Query("select t from SupportTicket t where lower(t.radicado) like lower(concat('%', :q, '%')) or lower(t.numeroDocumento) like lower(concat('%', :q, '%')) or lower(t.nombreCompleto) like lower(concat('%', :q, '%')) or lower(t.email) like lower(concat('%', :q, '%'))")
    List<SupportTicket> searchPQRS(@Param("q") String q);
}