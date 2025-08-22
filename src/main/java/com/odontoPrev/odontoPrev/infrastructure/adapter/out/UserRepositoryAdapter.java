package com.odontoPrev.odontoPrev.infrastructure.adapter.out;

import com.odontoPrev.odontoPrev.application.port.out.LoadUserPort;
import com.odontoPrev.odontoPrev.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements LoadUserPort {

    private final SpringDataUserRepository repository;

    @Override
    public Optional<User> loadUserByUsername(String username) {
        return repository.findByUsername(username);
    }
}