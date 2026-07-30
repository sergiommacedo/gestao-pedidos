package br.com.sergio.gestaopedidos.dto.pedido;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
@Schema(description = "Item utilizado no cadastro de um pedido")
public record ItemPedidoRequest(

        @Schema(
                description = "Identificador do produto",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Produto é obrigatório.")
        Long produtoId,

        @Schema(
                description = "Quantidade do produto",
                example = "2",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Quantidade é obrigatória.")
        @Min(value = 1, message = "Quantidade deve ser maior que zero.")
        Integer quantidade

) {
}