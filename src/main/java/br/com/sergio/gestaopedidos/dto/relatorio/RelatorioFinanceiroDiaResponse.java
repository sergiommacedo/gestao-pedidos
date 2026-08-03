package br.com.sergio.gestaopedidos.dto.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RelatorioFinanceiroDiaResponse(
        LocalDate data,
        Long pedidosValidos,
        Long cancelados,
        BigDecimal faturamentoProdutos,
        BigDecimal taxasEntrega,
        BigDecimal faturamentoTotal,
        BigDecimal ticketMedio
) {
}
