package br.com.sergio.gestaopedidos.dto.pedido;

import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "Dados retornados de um pedido")
public record PedidoResponse(

        @Schema(description = "Identificador do pedido", example = "1")
        Long id,

        @Schema(description = "Identificador do cliente", example = "5")
        Long clienteId,

        @Schema(description = "Nome do cliente", example = "João da Silva")
        String clienteNome,

        @Schema(description = "Status atual do pedido", example = "ABERTO")
        StatusPedido status,

        @Schema(description = "Forma de pagamento", example = "PIX")
        FormaPagamento formaPagamento,

        @Schema(description = "Tipo de entrega", example = "ENTREGA")
        TipoEntrega tipoEntrega,

        @Schema(description = "Subtotal dos itens", example = "85.90")
        BigDecimal subtotal,

        @Schema(description = "Taxa de entrega", example = "8.00")
        BigDecimal taxaEntrega,

        @Schema(description = "Valor total do pedido", example = "93.90")
        BigDecimal valorTotal,

        @Schema(description = "Observações do pedido", example = "Sem cebola.")
        String observacao,

        @Schema(description = "Data e hora de criação", example = "2026-07-30T11:30:00")
        LocalDateTime criadoEm,

        @Schema(description = "Itens do pedido")
        List<ItemPedidoResponse> itens

) {
}