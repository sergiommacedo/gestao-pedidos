package br.com.sergio.gestaopedidos.dto.usuario;

import br.com.sergio.gestaopedidos.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UsuarioRequest(

        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String nome,

        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres.")
        String senha,

        @NotNull(message = "Perfil é obrigatório.")
        PerfilUsuario perfil,

        @NotNull(message = "Informe se o usuário está ativo.")
        Boolean ativo

) {
}