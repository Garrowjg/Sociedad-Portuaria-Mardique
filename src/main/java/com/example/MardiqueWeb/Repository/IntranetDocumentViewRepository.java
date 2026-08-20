package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.IntranetDocumentView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntranetDocumentViewRepository extends JpaRepository<IntranetDocumentView, Long> {

    List<IntranetDocumentView> findByDocumentIdOrderByViewedAtDesc(Long documentId);

    List<IntranetDocumentView> findByDocumentIdOrderByViewedAtAsc(Long documentId);

    List<IntranetDocumentView> findAllByOrderByViewedAtDesc();

    List<IntranetDocumentView> findAllByOrderByViewedAtAsc();

    long countByDocumentId(Long documentId);
}