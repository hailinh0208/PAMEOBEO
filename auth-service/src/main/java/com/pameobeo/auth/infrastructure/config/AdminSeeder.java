package com.pameobeo.auth.infrastructure.config;

import com.pameobeo.auth.application.port.out.PasswordHasher;
import com.pameobeo.auth.application.port.out.UserRepository;
import com.pameobeo.auth.domain.model.User;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** ADMIN accounts are never created through registration (UC01 - BR3); one is seeded on startup. */
@Component
class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final AuthProperties props;
    private final Clock clock;

    AdminSeeder(UserRepository users, PasswordHasher passwordHasher, AuthProperties props, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.props = props;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        AuthProperties.Admin admin = props.admin();
        String email = User.normalizeEmail(admin.email());
        if (!users.existsByEmail(email)) {
            users.save(User.createAdmin(email, passwordHasher.hash(admin.password()), admin.fullName(), clock.instant()));
            log.info("Seeded admin account {}", email);
        }
    }
}
