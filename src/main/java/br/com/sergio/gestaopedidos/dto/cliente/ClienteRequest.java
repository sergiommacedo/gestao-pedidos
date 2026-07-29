package br.com.sergio.gestaopedidos.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ClienteRequest(

        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "Telefone é obrigatório.")
        @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres.")
        String telefone,

        @Size(max = 150, message = "Endereço deve ter no máximo 150 caracteres.")
        String endereco,

        @Size(max = 20, message = "Número deve ter no máximo 20 caracteres.")
        String numero,

        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres.")
        String bairro,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres.")
        String cidade,

        @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres.")
        String cep,

        @Size(max = 150, message = "Complemento deve ter no máximo 150 caracteres.")
        String complemento

) {
}