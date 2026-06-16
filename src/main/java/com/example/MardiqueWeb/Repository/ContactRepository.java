package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByNombreContainingIgnoreCase(String nombre);
}
