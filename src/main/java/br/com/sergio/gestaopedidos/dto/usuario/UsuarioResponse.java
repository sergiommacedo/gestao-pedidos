package br.com.sergio.gestaopedidos.dto.usuario;

import br.com.sergio.gestaopedidos.enums.PerfilUsuario;
import lombok.Builder;

@Builder
public record UsuarioResponse(

        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        Boolean ativo

) {
}