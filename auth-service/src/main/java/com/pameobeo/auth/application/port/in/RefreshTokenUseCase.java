package com.pameobeo.auth.application.port.in;

public interface RefreshTokenUseCase {

    TokenPair refresh(String refreshToken);
}
