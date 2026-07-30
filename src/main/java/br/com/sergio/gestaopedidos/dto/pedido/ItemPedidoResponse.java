package br.com.sergio.gestaopedidos.dto.pedido;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "Item retornado em um pedido")
public record ItemPedidoResponse(

        @Schema(description = "Identificador do item", example = "1")
        Long id,

        @Schema(description = "Identificador do produto", example = "10")
        Long produtoId,

        @Schema(description = "Nome do produto", example = "Feijoada Grande")
        String produtoNome,

        @Schema(description = "Quantidade", example = "2")
        Integer quantidade,

        @Schema(description = "Valor unitário do produto", example = "49.90")
        BigDecimal valorUnitario,

        @Schema(description = "Subtotal do item", example = "99.80")
        BigDecimal subtotal

) {
}