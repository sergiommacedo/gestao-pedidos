package br.com.sergio.gestaopedidos.dto.usuario;

import br.com.sergio.gestaopedidos.enums.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Dados retornados de um usuário")
public record UsuarioResponse(

        @Schema(description = "Identificador do usuário", example = "1")
        Long id,

        @Schema(description = "Nome completo do usuário", example = "Sérgio Macedo")
        String nome,

        @Schema(description = "E-mail do usuário", example = "sergio@email.com")
        String email,

        @Schema(description = "Perfil do usuário", example = "ADMIN")
        PerfilUsuario perfil,

        @Schema(description = "Indica se o usuário está ativo", example = "true")
        Boolean ativo,

        @Schema(description = "Indica se a troca da senha inicial está pendente", example = "false")
        Boolean trocarSenhaPrimeiroAcesso

) {
}
