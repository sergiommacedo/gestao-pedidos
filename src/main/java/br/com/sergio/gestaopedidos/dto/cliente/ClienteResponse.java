package br.com.sergio.gestaopedidos.dto.cliente;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Dados retornados de um cliente")
public record ClienteResponse(

        @Schema(
                description = "Identificador único do cliente",
                example = "1"
        )
        Long id,

        @Schema(
                description = "Nome completo do cliente",
                example = "João da Silva"
        )
        String nome,

        @Schema(
                description = "Telefone para contato",
                example = "(41) 99999-9999"
        )
        String telefone,

        @Schema(
                description = "Nome da rua",
                example = "Rua das Flores"
        )
        String endereco,

        @Schema(
                description = "Número do endereço",
                example = "123"
        )
        String numero,

        @Schema(
                description = "Bairro",
                example = "Centro"
        )
        String bairro,

        @Schema(
                description = "Cidade",
                example = "Curitiba"
        )
        String cidade,

        @Schema(
                description = "CEP",
                example = "80000-000"
        )
        String cep,

        @Schema(
                description = "Complemento do endereço",
                example = "Apto 101"
        )
        String complemento

) {
}