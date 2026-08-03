package br.com.sergio.gestaopedidos.dto.relatorio;

import java.math.BigDecimal;

public record RelatorioPedidoIndicadoresResponse(
        long totalPedidos,
        long pedidosValidos,
        long cancelados,
        BigDecimal faturamento,
        BigDecimal taxasEntrega,
        BigDecimal ticketMedio
) {

    public static RelatorioPedidoIndicadoresResponse vazio() {
        return new RelatorioPedidoIndicadoresResponse(
                0,
                0,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
