package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.CarouselEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarouselEntryRepository extends JpaRepository<CarouselEntry, Long> {
    List<CarouselEntry> findBySectionOrderByIdAsc(String section);
}