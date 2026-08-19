package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.IntranetDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntranetDocumentRepository extends JpaRepository<IntranetDocument, Long> {

    // Raíz del sector: carpetas y archivos sin padre (vista de sector)
    List<IntranetDocument> findBySectorAndParentIdIsNullOrderByUploadedAtDesc(String sector);

    // Contenido de una carpeta: hijos con ese parentId
    List<IntranetDocument> findByParentIdOrderByUploadedAtDesc(Long parentId);

    // Cuenta hijos (archivos + carpetas) de una carpeta
    long countByParentId(Long parentId);

    // Todos los hijos recursivos de una carpeta (para borrado en cascada)
    List<IntranetDocument> findByParentId(Long parentId);

    // Documentos de un sector incluyendo los que están dentro de carpetas (para conteo total)
    List<IntranetDocument> findBySector(String sector);
}