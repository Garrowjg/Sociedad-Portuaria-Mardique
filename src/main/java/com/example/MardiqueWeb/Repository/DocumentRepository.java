package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByTipo(String tipo);
    Optional<Document> findByTipoAndCardKey(String tipo, String cardKey);
    List<Document> findByTipoAndCardKeyIsNotNull(String tipo);
}
