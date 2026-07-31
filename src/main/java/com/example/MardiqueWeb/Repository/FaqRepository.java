package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findByActivoTrueOrderByOrdenAsc();
    List<Faq> findAllByOrderByOrdenAsc();
}
