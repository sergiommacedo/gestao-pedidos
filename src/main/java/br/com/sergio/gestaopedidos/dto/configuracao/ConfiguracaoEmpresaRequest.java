package br.com.sergio.gestaopedidos.dto.configuracao;

import br.com.sergio.gestaopedidos.enums.TemaSistema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfiguracaoEmpresaRequest(

        @NotBlank(message = "Nome da empresa é obrigatório.")
        @Size(max = 150, message = "Nome da empresa deve ter no máximo 150 caracteres.")
        String nomeEmpresa,

        @NotBlank(message = "Nome curto é obrigatório.")
        @Size(max = 60, message = "Nome curto deve ter no máximo 60 caracteres.")
        String nomeCurto,

        @NotNull(message = "Tema é obrigatório.")
        TemaSistema tema,

        @Size(max = 255, message = "Texto de boas-vindas deve ter no máximo 255 caracteres.")
        String textoBoasVindas
) {
}
