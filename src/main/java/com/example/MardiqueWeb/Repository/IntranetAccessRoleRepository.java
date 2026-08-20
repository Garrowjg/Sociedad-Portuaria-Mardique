package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.IntranetAccessRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IntranetAccessRoleRepository extends JpaRepository<IntranetAccessRole, Long> {

    List<IntranetAccessRole> findByRole(String role);

    Optional<IntranetAccessRole> findByEmail(String email);

    boolean existsByEmailAndRole(String email, String role);

    void deleteByEmail(String email);
}