package br.com.sergio.gestaopedidos.dto.pedido;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ItemPedidoRequest(

        @NotNull(message = "Produto é obrigatório.")
        Long produtoId,

        @NotNull(message = "Quantidade é obrigatória.")
        @Min(value = 1, message = "Quantidade deve ser maior que zero.")
        Integer quantidade

) {
}