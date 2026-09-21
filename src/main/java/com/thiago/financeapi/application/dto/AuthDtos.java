package com.thiago.financeapi.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 180) String email,
            @NotBlank @Size(min = 8, max = 72)
            @Schema(description = "Minimo de 8 caracteres", example = "senhaForte123")
            String password) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresIn) {

        public static TokenResponse bearer(String token, long expiresIn) {
            return new TokenResponse(token, "Bearer", expiresIn);
        }
    }
}
