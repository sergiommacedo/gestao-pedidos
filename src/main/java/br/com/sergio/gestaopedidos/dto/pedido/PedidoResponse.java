package br.com.sergio.gestaopedidos.dto.pedido;

import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PedidoResponse(

        Long id,
        Long clienteId,
        String clienteNome,
        StatusPedido status,
        FormaPagamento formaPagamento,
        TipoEntrega tipoEntrega,
        BigDecimal subtotal,
        BigDecimal taxaEntrega,
        BigDecimal valorTotal,
        String observacao,
        LocalDateTime criadoEm,
        List<ItemPedidoResponse> itens

) {
}