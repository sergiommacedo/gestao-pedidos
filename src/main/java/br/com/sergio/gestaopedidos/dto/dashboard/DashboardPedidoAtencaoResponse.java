package br.com.sergio.gestaopedidos.dto.dashboard;

import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;

import java.math.BigDecimal;

public record DashboardPedidoAtencaoResponse(
        Long id,
        String clienteNome,
        TipoEntrega tipoEntrega,
        StatusPedido status,
        BigDecimal valorTotal,
        Boolean estoqueMovimentado
) {
    public String situacaoEstoque() { return Boolean.TRUE.equals(estoqueMovimentado) ? "Movimentado" : "Pendente"; }
}
