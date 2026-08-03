package br.com.sergio.gestaopedidos.dto.relatorio;

import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import java.math.BigDecimal;

public record RelatorioFinanceiroPagamentoResponse(
        FormaPagamento formaPagamento,
        Long pedidos,
        BigDecimal faturamento,
        BigDecimal participacaoPercentual,
        BigDecimal ticketMedio
) {
}
