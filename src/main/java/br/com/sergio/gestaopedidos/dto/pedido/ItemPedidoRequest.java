package br.com.sergio.gestaopedidos.dto.pedido;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

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
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero.")
        @Digits(
                integer = 7,
                fraction = 3,
                message = "Quantidade deve possuir no máximo 3 casas decimais."
        )
        BigDecimal quantidade,

        BigDecimal precoOriginal,

        @DecimalMin(value = "0.00", message = "Desconto do item não pode ser negativo.")
        @DecimalMax(value = "100.00", message = "Desconto do item não pode ultrapassar 100%.")
        @Digits(integer = 3, fraction = 4, message = "Desconto do item possui formato inválido.")
        BigDecimal percentualDesconto,

        @DecimalMin(value = "0.00", message = "Preço final não pode ser negativo.")
        @Digits(integer = 8, fraction = 2, message = "Preço final deve possuir no máximo 2 casas decimais.")
        BigDecimal precoFinal,

        @Schema(description = "Observação do item", example = "Sem farofa")
        @Size(max = 255, message = "Observação do item deve possuir no máximo 255 caracteres.")
        String observacao

) {
}
