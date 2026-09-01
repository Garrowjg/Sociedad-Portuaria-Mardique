package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.IntranetHrDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IntranetHrDocumentRepository extends JpaRepository<IntranetHrDocument, Long> {
    List<IntranetHrDocument> findBySectionIdOrderByUploadedAtDesc(String sectionId);
    void deleteBySectionId(String sectionId);
}
