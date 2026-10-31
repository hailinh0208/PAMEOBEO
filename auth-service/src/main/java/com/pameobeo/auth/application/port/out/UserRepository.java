package com.pameobeo.auth.application.port.out;

import com.pameobeo.auth.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    User save(User user);
}
