package br.com.sergio.gestaopedidos.dto.usuario;

import br.com.sergio.gestaopedidos.enums.PerfilUsuario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioWebForm {

    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
    private String nome;

    @NotBlank(message = "Login ou e-mail é obrigatório.")
    @Size(max = 100, message = "Login ou e-mail deve ter no máximo 100 caracteres.")
    private String login;

    @NotNull(message = "Perfil é obrigatório.")
    private PerfilUsuario perfil;

    @Builder.Default
    private Boolean ativo = true;

    @Builder.Default
    private Boolean trocarSenhaPrimeiroAcesso = false;

    private String senha;

    private String confirmarSenha;
}
