package br.com.sergio.gestaopedidos.dto.dashboard;

import br.com.sergio.gestaopedidos.enums.StatusPedido;

import java.math.BigDecimal;

public record DashboardStatusResponse(
        StatusPedido status,
        String descricao,
        long quantidade,
        BigDecimal percentual
) {
}
