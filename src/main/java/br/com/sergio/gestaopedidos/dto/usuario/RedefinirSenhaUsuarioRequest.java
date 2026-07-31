package br.com.sergio.gestaopedidos.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaUsuarioRequest(
        @NotBlank(message = "Nova senha é obrigatória.")
        @Size(min = 6, message = "Nova senha deve ter no mínimo 6 caracteres.")
        String novaSenha,

        @NotBlank(message = "Confirmação da senha é obrigatória.")
        String confirmarSenha,

        Boolean obrigarTroca
) {
}
