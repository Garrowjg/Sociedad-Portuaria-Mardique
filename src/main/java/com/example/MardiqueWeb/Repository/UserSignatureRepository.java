package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.UserSignature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserSignatureRepository extends JpaRepository<UserSignature, Long> {
    Optional<UserSignature> findByEmail(String email);
}
