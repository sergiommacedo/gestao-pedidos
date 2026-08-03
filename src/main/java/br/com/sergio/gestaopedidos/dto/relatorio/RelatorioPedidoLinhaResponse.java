package br.com.sergio.gestaopedidos.dto.relatorio;

import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RelatorioPedidoLinhaResponse(
        Long id,
        LocalDate dataAgendada,
        LocalDateTime dataPedido,
        String clienteNome,
        String clienteTelefone,
        StatusPedido status,
        TipoEntrega tipoEntrega,
        FormaPagamento formaPagamento,
        BigDecimal subtotal,
        BigDecimal taxaEntrega,
        BigDecimal valorTotal
) {
}
