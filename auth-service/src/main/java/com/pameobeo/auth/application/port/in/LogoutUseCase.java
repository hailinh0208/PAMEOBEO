package com.pameobeo.auth.application.port.in;

public interface LogoutUseCase {

    void logout(String refreshToken);
}
