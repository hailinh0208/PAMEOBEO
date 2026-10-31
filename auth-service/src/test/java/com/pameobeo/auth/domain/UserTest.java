package com.pameobeo.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pameobeo.auth.domain.exception.AccountTemporarilyLockedException;
import com.pameobeo.auth.domain.model.LoginPolicy;
import com.pameobeo.auth.domain.model.Role;
import com.pameobeo.auth.domain.model.User;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final LoginPolicy POLICY = new LoginPolicy(5, Duration.ofMinutes(15));
    private static final Instant NOW = Instant.parse("2026-10-28T03:00:00Z");

    @Test
    void registeredUserIsAlwaysCustomerWithNormalizedEmail() {
        User user = User.registerCustomer("  Linh@Example.COM ", "hash", "Linh", NOW);

        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(user.getEmail()).isEqualTo("linh@example.com");
    }

    @Test
    void fifthConsecutiveFailureLocksAccountFor15Minutes() {
        User user = User.registerCustomer("a@b.c", "hash", "A", NOW);

        for (int i = 0; i < 4; i++) {
            user.recordFailedLogin(NOW, POLICY);
        }
        assertThat(user.isLocked(NOW)).isFalse();

        user.recordFailedLogin(NOW, POLICY);

        assertThat(user.isLocked(NOW)).isTrue();
        assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void lockedAccountIsRejectedEvenBeforePasswordCheck() {
        User user = User.registerCustomer("a@b.c", "hash", "A", NOW);
        for (int i = 0; i < 5; i++) {
            user.recordFailedLogin(NOW, POLICY);
        }

        assertThatThrownBy(() -> user.ensureNotLocked(NOW.plusSeconds(60)))
                .isInstanceOf(AccountTemporarilyLockedException.class);
    }

    @Test
    void successfulLoginResetsCounter() {
        User user = User.registerCustomer("a@b.c", "hash", "A", NOW);
        user.recordFailedLogin(NOW, POLICY);
        user.recordFailedLogin(NOW, POLICY);
        user.recordFailedLogin(NOW, POLICY);

        user.recordSuccessfulLogin();

        assertThat(user.getFailedLoginAttempts()).isZero();
    }
}
