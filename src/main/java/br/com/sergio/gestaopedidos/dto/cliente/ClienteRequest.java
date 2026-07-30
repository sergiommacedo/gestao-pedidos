package br.com.sergio.gestaopedidos.dto.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
@Schema(description = "Dados necessários para cadastro ou atualização de um cliente")
public record ClienteRequest(

        @Schema(
                description = "Nome completo do cliente",
                example = "João da Silva"
        )
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String nome,

        @Schema(
                description = "Telefone para contato",
                example = "(41) 99999-9999"
        )
        @NotBlank(message = "Telefone é obrigatório.")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        String telefone,

        @Schema(
                description = "Nome da rua",
                example = "Rua das Flores"
        )
        @Size(max = 150, message = "Endereço deve ter no máximo 150 caracteres.")
        String endereco,

        @Schema(
                description = "Número do endereço",
                example = "123"
        )
        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres.")
        String numero,

        @Schema(
                description = "Bairro",
                example = "Centro"
        )
        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres.")
        String bairro,

        @Schema(
                description = "Cidade",
                example = "Curitiba"
        )
        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres.")
        String cidade,

        @Schema(
                description = "CEP",
                example = "80000-000"
        )
        @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres.")
        String cep,

        @Schema(
                description = "Complemento do endereço",
                example = "Apto 101"
        )
        @Size(max = 150, message = "Complemento deve ter no máximo 150 caracteres.")
        String complemento

) {
}