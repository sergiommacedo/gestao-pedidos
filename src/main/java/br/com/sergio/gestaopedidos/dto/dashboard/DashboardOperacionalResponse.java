package br.com.sergio.gestaopedidos.dto.dashboard;

import br.com.sergio.gestaopedidos.dto.resumo.ResumoProdutoVendidoResponse;
import br.com.sergio.gestaopedidos.enums.StatusProducao;
import br.com.sergio.gestaopedidos.enums.TipoCompra;
import br.com.sergio.gestaopedidos.enums.TipoItemEstoque;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardOperacionalResponse(
        LocalDate data,
        DashboardIndicadoresResponse pedidos,
        List<DashboardPedidoAtencaoResponse> pedidosAtencao,
        List<DashboardStatusResponse> resumoStatus,
        ProducaoDia producao,
        EstoqueResumo estoque,
        ComprasDia compras,
        List<ResumoProdutoVendidoResponse> produtosVendidos,
        List<Alerta> alertas
) {
    public record ProducaoDia(Long id, StatusProducao status, int produtosDistintos,
                              BigDecimal quantidadeTotal, BigDecimal custoReal,
                              LocalDateTime confirmadaEm) {
        public boolean existe() { return id != null; }
    }

    public record EstoqueResumo(long itensComSaldo, long abaixoDoMinimo, long semEstoque,
                                long produtosProduzidosDisponiveis, long produtosRevendaDisponiveis,
                                BigDecimal valorInsumos, BigDecimal valorRevenda,
                                BigDecimal valorProduzidos, BigDecimal valorTotal,
                                List<ItemEstoque> alertas, List<ItemEstoque> produtosProduzidos) {}

    public record ItemEstoque(TipoItemEstoque categoria, Long referenciaId, String nome,
                              UnidadeMedida unidade, BigDecimal saldo, BigDecimal minimo,
                              String situacao) {}

    public record ComprasDia(long quantidade, BigDecimal valorTotal, long comprasInsumos,
                             BigDecimal valorInsumos, long comprasRevenda,
                             BigDecimal valorRevenda) {
        public boolean existe() { return quantidade > 0; }
    }

    public record Alerta(String icone, String nivel, String titulo, String descricao,
                         String url, boolean somenteAdmin) {}
}
