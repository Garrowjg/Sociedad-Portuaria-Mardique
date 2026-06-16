package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.PageContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PageContentRepository extends JpaRepository<PageContent, Long> {
    List<PageContent> findByPage(String page);
    Optional<PageContent> findByPageAndSectionKey(String page, String sectionKey);
}
