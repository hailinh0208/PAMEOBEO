package com.pameobeo.auth.application.port.in;

public interface LoginUseCase {

    TokenPair login(LoginCommand command);

    record LoginCommand(String email, String password) {}
}
