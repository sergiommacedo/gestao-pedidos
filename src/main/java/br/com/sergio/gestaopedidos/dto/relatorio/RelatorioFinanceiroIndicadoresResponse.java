package br.com.sergio.gestaopedidos.dto.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RelatorioFinanceiroIndicadoresResponse(
        long pedidosTotais,
        long pedidosValidos,
        long cancelados,
        BigDecimal faturamentoProdutos,
        BigDecimal taxasEntrega,
        BigDecimal faturamentoBruto,
        BigDecimal ticketMedio,
        BigDecimal valorCancelado,
        LocalDate melhorDia,
        BigDecimal melhorDiaValor
) {
    public static RelatorioFinanceiroIndicadoresResponse vazio() {
        return new RelatorioFinanceiroIndicadoresResponse(
                0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO
        );
    }
}
