package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.IntranetDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntranetDocumentRepository extends JpaRepository<IntranetDocument, Long> {

    List<IntranetDocument> findBySectorOrderByUploadedAtDesc(String sector);
}