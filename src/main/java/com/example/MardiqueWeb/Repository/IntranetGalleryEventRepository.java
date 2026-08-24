package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.IntranetGalleryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IntranetGalleryEventRepository extends JpaRepository<IntranetGalleryEvent, Long> {
    List<IntranetGalleryEvent> findAllByOrderByCreatedAtDesc();
}
