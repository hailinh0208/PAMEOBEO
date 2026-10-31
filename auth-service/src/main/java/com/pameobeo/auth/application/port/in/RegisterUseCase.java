package com.pameobeo.auth.application.port.in;

import java.util.UUID;

public interface RegisterUseCase {

    UUID register(RegisterCommand command);

    record RegisterCommand(String email, String password, String fullName) {}
}
