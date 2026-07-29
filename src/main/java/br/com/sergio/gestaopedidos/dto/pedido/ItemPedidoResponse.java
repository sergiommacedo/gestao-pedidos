package br.com.sergio.gestaopedidos.dto.pedido;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ItemPedidoResponse(

        Long id,
        Long produtoId,
        String produtoNome,
        Integer quantidade,
        BigDecimal valorUnitario,
        BigDecimal subtotal

) {
}