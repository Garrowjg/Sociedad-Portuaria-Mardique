package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.IntranetArea;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IntranetAreaRepository extends JpaRepository<IntranetArea, Long> {
    List<IntranetArea> findAllByOrderByNombreAsc();
}
