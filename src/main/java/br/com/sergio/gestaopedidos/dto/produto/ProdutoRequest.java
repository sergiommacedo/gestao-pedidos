package br.com.sergio.gestaopedidos.dto.produto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProdutoRequest(

        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres.")
        String descricao,

        @NotNull(message = "Preço é obrigatório.")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero.")
        BigDecimal preco,

        @NotNull(message = "Informe se o produto está ativo.")
        Boolean ativo

) {
}