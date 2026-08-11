package br.com.sergio.gestaopedidos.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardAnaliticoResponse(long produtosVendidos, long revendasVendidas,
        long pedidosAtrasados, long pedidosEntregues, BigDecimal lucroBrutoEstimado,
        List<SerieDia> vendasPorDia, List<SerieDia> producaoPorDia,
        List<RankingCliente> rankingClientes, List<Ranking> consumoInsumos) {
    public record SerieDia(LocalDate data, BigDecimal valor) {}
    public record RankingCliente(Long clienteId, String nome, long quantidadePedidos, BigDecimal valorTotal,
                                 BigDecimal ticketMedio) {}
    public record Ranking(String nome, BigDecimal valor) {}
}
