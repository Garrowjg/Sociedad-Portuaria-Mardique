package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.IntranetHrSectionView;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IntranetHrSectionViewRepository extends JpaRepository<IntranetHrSectionView, Long> {
    List<IntranetHrSectionView> findBySectionIdOrderByViewedAtDesc(String sectionId);
    long countBySectionId(String sectionId);
    void deleteBySectionId(String sectionId);
}
