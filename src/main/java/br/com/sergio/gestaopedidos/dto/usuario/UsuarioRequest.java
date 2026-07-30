package br.com.sergio.gestaopedidos.dto.usuario;

import br.com.sergio.gestaopedidos.enums.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Dados necessários para cadastro ou atualização de um usuário")
public record UsuarioRequest(

        @Schema(
                description = "Nome completo do usuário",
                example = "Sérgio Macedo",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String nome,

        @Schema(
                description = "E-mail do usuário",
                example = "sergio@email.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "E-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres.")
        String email,

        @Schema(
                description = "Senha de acesso",
                example = "123456",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres.")
        String senha,

        @Schema(
                description = "Perfil do usuário",
                example = "ADMIN",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Perfil é obrigatório.")
        PerfilUsuario perfil,

        @Schema(
                description = "Indica se o usuário está ativo",
                example = "true"
        )
        Boolean ativo

) {
}