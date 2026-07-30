package br.com.sergio.gestaopedidos.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta de autenticação bem-sucedida")
public record LoginResponse(

        @Schema(
                description = "Token JWT utilizado para acessar a API",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String token,

        @Schema(
                description = "Tipo do token",
                example = "Bearer"
        )
        String tipo
) {
}