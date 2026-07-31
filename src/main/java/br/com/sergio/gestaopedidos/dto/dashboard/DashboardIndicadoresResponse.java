package br.com.sergio.gestaopedidos.dto.dashboard;

import java.math.BigDecimal;

public record DashboardIndicadoresResponse(
        long agendadosHoje,
        long emPreparacao,
        long saiuParaEntrega,
        BigDecimal faturamentoDoDia
) {
}
