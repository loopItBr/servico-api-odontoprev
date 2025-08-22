package com.odontoPrev.odontoPrev.infrastructure.adapter.out;

import com.odontoPrev.odontoPrev.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}