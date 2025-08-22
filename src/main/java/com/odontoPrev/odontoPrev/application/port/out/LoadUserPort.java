package com.odontoPrev.odontoPrev.application.port.out;

import com.odontoPrev.odontoPrev.domain.model.User;
import java.util.Optional;

public interface LoadUserPort {
    Optional<User> loadUserByUsername(String username);
}

