package com.example.MardiqueWeb.Repository;

import com.example.MardiqueWeb.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByEnabled(boolean enabled);
    long countByTipo(String tipo);
    List<User> findByRole(String role);
}
