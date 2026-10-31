package com.pameobeo.auth.infrastructure.persistence;

import com.pameobeo.auth.application.port.out.UserRepository;
import com.pameobeo.auth.domain.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository jpa;

    UserRepositoryAdapter(SpringDataUserRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserJpaEntity::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public User save(User user) {
        return jpa.save(UserJpaEntity.fromDomain(user)).toDomain();
    }
}
