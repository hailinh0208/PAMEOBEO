package com.pameobeo.auth.interfaces.rest;

import com.pameobeo.auth.application.port.in.LoginUseCase;
import com.pameobeo.auth.application.port.in.LogoutUseCase;
import com.pameobeo.auth.application.port.in.RefreshTokenUseCase;
import com.pameobeo.auth.application.port.in.RegisterUseCase;
import com.pameobeo.auth.application.port.in.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;

    AuthController(RegisterUseCase registerUseCase, LoginUseCase loginUseCase,
                   RefreshTokenUseCase refreshTokenUseCase, LogoutUseCase logoutUseCase) {
        this.registerUseCase = registerUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> register(@Valid @RequestBody RegisterRequest request) {
        UUID id = registerUseCase.register(
                new RegisterUseCase.RegisterCommand(request.email(), request.password(), request.fullName()));
        return Map.of("userId", id);
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(loginUseCase.login(new LoginUseCase.LoginCommand(request.email(), request.password())));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(refreshTokenUseCase.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody RefreshRequest request) {
        logoutUseCase.logout(request.refreshToken());
    }

    @GetMapping("/me")
    Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "userId", jwt.getSubject(),
                "email", jwt.getClaimAsString("email"),
                "name", jwt.getClaimAsString("name"),
                "roles", jwt.getClaimAsStringList("roles"));
    }

    record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
            @NotBlank String fullName) {}

    record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    record RefreshRequest(@NotBlank String refreshToken) {}

    record TokenResponse(String tokenType, String accessToken, Instant accessTokenExpiresAt,
                         String refreshToken, Instant refreshTokenExpiresAt) {
        static TokenResponse from(TokenPair pair) {
            return new TokenResponse("Bearer", pair.accessToken(), pair.accessTokenExpiresAt(),
                    pair.refreshToken(), pair.refreshTokenExpiresAt());
        }
    }
}
