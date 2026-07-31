package br.com.sergio.gestaopedidos.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaInicialRequest(
        @NotBlank(message = "Senha atual é obrigatória.")
        String senhaAtual,

        @NotBlank(message = "Nova senha é obrigatória.")
        @Size(min = 6, message = "Nova senha deve ter no mínimo 6 caracteres.")
        String novaSenha,

        @NotBlank(message = "Confirmação da nova senha é obrigatória.")
        String confirmarNovaSenha
) {
}
