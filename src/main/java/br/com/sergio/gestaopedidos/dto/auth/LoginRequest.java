package br.com.sergio.gestaopedidos.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados necessários para autenticação")
public record LoginRequest(

        @Schema(
                description = "E-mail do usuário",
                example = "sergio@email.com"
        )
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        String email,

        @Schema(
                description = "Senha do usuário",
                example = "123456"
        )
        @NotBlank(message = "A senha é obrigatória.")
        String senha
) {
}