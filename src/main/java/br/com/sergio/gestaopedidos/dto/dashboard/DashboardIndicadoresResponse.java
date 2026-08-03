package br.com.sergio.gestaopedidos.dto.dashboard;

import java.math.BigDecimal;

public record DashboardIndicadoresResponse(
        long agendadosHoje,
        long validos,
        long cancelados,
        long emPreparacao,
        long saiuParaEntrega,
        BigDecimal faturamentoProdutos,
        BigDecimal taxasEntrega,
        BigDecimal faturamentoDoDia
) {
}
