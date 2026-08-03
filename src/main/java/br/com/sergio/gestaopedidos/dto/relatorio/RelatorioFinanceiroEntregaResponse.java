package br.com.sergio.gestaopedidos.dto.relatorio;

import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import java.math.BigDecimal;

public record RelatorioFinanceiroEntregaResponse(
        TipoEntrega tipoEntrega,
        Long pedidos,
        BigDecimal faturamentoProdutos,
        BigDecimal taxasEntrega,
        BigDecimal faturamentoTotal,
        BigDecimal participacaoPercentual
) {
}
