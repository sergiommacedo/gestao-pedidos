package br.com.sergio.gestaopedidos.dto.pedido;

import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "Dados necessários para cadastro de um pedido")
public record PedidoRequest(

        @Schema(
                description = "Identificador do cliente",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Cliente é obrigatório.")
        Long clienteId,

        @Schema(
                description = "Data agendada para o pedido",
                example = "2026-08-01",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Data agendada é obrigatória.")
        @FutureOrPresent(message = "Data agendada não pode ser anterior à data atual.")
        LocalDate dataAgendada,

        @Schema(
                description = "Forma de pagamento",
                example = "PIX",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Forma de pagamento é obrigatória.")
        FormaPagamento formaPagamento,

        @Schema(
                description = "Tipo de entrega",
                example = "ENTREGA",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Tipo de entrega é obrigatório.")
        TipoEntrega tipoEntrega,

        @Schema(
                description = "Taxa de entrega",
                example = "8.00"
        )
        @DecimalMin(
                value = "0.00",
                message = "Taxa de entrega não pode ser negativa."
        )
        BigDecimal taxaEntrega,

        @Schema(
                description = "Observações do pedido",
                example = "Sem cebola."
        )
        @Size(
                max = 500,
                message = "Observação deve possuir no máximo 500 caracteres."
        )
        String observacao,

        @Schema(
                description = "Lista de itens do pedido",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Valid
        @NotEmpty(message = "O pedido deve possuir pelo menos um item.")
        List<ItemPedidoRequest> itens

) {
}
