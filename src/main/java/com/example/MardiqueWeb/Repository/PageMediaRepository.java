package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.PageMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PageMediaRepository extends JpaRepository<PageMedia, Long> {
    List<PageMedia> findByPageOrderByIdAsc(String page);
    Optional<PageMedia> findByPageAndMediaKey(String page, String mediaKey);
}