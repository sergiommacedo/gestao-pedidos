package br.com.sergio.gestaopedidos.dto.dashboard;

import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record HistoricoClientePedidosResponse(
        Long clienteId, String clienteNome, long quantidadeTotal, BigDecimal valorTotal,
        BigDecimal ticketMedio, List<Pedido> pedidos, int pagina, int totalPaginas,
        Periodo periodo, LocalDate dataInicial, LocalDate dataFinal
) {
    public enum Periodo { ULTIMOS_7_DIAS, TODO_HISTORICO }

    public record Pedido(Long id, LocalDate data, LocalTime horario, TipoEntrega tipoEntrega,
                         StatusPedido status, BigDecimal subtotal, BigDecimal percentualDescontoGeral,
                         BigDecimal valorDescontoGeral, BigDecimal taxaEntrega,
                         BigDecimal valorTotal) {}

    public record Item(String produtoNome, BigDecimal quantidade, UnidadeVenda unidade,
                       BigDecimal precoUnitarioOriginal, BigDecimal percentualDesconto,
                       BigDecimal precoUnitario, BigDecimal valorDescontoUnitario, BigDecimal subtotal) {}

    public record DetalhesItens(List<Item> itens, BigDecimal subtotal, BigDecimal percentualDescontoGeral,
                                BigDecimal valorDescontoGeral,
                                BigDecimal taxaEntrega, BigDecimal valorTotal) {}
}
