package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.resumo.ResumoProdutoVendidoResponse;
import br.com.sergio.gestaopedidos.dto.resumo.ResumoVendasDiaResponse;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.repository.ItemPedidoRepository;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import br.com.sergio.gestaopedidos.repository.*;
import br.com.sergio.gestaopedidos.dto.dashboard.DashboardOperacionalResponse;
import br.com.sergio.gestaopedidos.dto.dashboard.DashboardPedidoAtencaoResponse;
import br.com.sergio.gestaopedidos.enums.*;
import java.time.LocalDateTime;
import br.com.sergio.gestaopedidos.util.FormatacaoUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceTest {

    @Test
    void dashboardVazioPermaneceOperacionalSemAlertas() {
        LocalDate data = LocalDate.of(2026, 8, 3);
        PedidoRepository.ResumoDashboard pedidos = proxy(PedidoRepository.ResumoDashboard.class,
                Map.of("getTotal",0L,"getValidos",0L,"getCancelados",0L,"getEmPreparacao",0L,
                        "getSaiuParaEntrega",0L,"getProdutos",BigDecimal.ZERO,"getTaxas",BigDecimal.ZERO,
                        "getFaturamento",BigDecimal.ZERO));
        PedidoRepository pedidoRepository = proxy(PedidoRepository.class, Map.of(
                "resumirDashboard", pedidos, "buscarPedidosQuePrecisamAtencao", List.of(),
                "contarPedidosPorStatusNaData", List.of()));
        ItemPedidoRepository itens = proxy(ItemPedidoRepository.class,
                Map.of("resumirProdutosMaisVendidosDashboard", List.of()));
        ProducaoRepository producoes = proxy(ProducaoRepository.class, Map.of("resumirDashboard", Optional.empty()));
        SaldoEstoqueRepository.ResumoDashboard estoqueResumo = proxy(SaldoEstoqueRepository.ResumoDashboard.class,
                Map.of("getItensComSaldo",0L,"getAbaixoDoMinimo",0L,"getSemEstoque",0L,
                        "getProduzidosDisponiveis",0L,"getRevendaDisponiveis",0L,"getValorInsumos",BigDecimal.ZERO,
                        "getValorRevenda",BigDecimal.ZERO,"getValorProduzidos",BigDecimal.ZERO,"getValorTotal",BigDecimal.ZERO));
        SaldoEstoqueRepository estoque = proxy(SaldoEstoqueRepository.class, Map.of(
                "resumirDashboard", estoqueResumo, "listarAlertasDashboard", List.of(),
                "listarProduzidosDisponiveisDashboard", List.of()));
        CompraRepository.ResumoDashboard comprasResumo = proxy(CompraRepository.ResumoDashboard.class,
                Map.of("getQuantidade",0L,"getValorTotal",BigDecimal.ZERO,"getComprasInsumos",0L,
                        "getValorInsumos",BigDecimal.ZERO,"getComprasRevenda",0L,"getValorRevenda",BigDecimal.ZERO));
        CompraRepository compras = proxy(CompraRepository.class, Map.of("resumirDashboard", comprasResumo));
        FichaTecnicaRepository fichas = proxy(FichaTecnicaRepository.class,
                Map.of("contarAtivasComCustoPendente",0L,"contarProdutosProduzidosAtivosSemFicha",0L));

        DashboardOperacionalResponse dashboard = new DashboardService(pedidoRepository, itens, producoes,
                estoque, compras, fichas).buscarDashboard(data);

        assertThat(dashboard.pedidos().agendadosHoje()).isZero();
        assertThat(dashboard.producao().existe()).isFalse();
        assertThat(dashboard.estoque().produtosProduzidos()).isEmpty();
        assertThat(dashboard.compras().existe()).isFalse();
        assertThat(dashboard.alertas()).isEmpty();
    }

    @Test
    void dashboardIntegraPedidosProducaoEstoqueComprasEAlertasSemCarregarEntidades() {
        LocalDate data = LocalDate.of(2026, 8, 3);
        PedidoRepository.ResumoDashboard pedidosResumo = proxy(PedidoRepository.ResumoDashboard.class,
                Map.of("getTotal",6L,"getValidos",5L,"getCancelados",1L,"getEmPreparacao",2L,
                        "getSaiuParaEntrega",1L,"getProdutos",new BigDecimal("450"),"getTaxas",new BigDecimal("30"),
                        "getFaturamento",new BigDecimal("480")));
        PedidoRepository.ContagemPedidosPorStatus pendentes = proxy(PedidoRepository.ContagemPedidosPorStatus.class,
                Map.of("getStatus",StatusPedido.PENDENTE,"getQuantidade",2L));
        PedidoRepository pedidos = proxy(PedidoRepository.class, Map.of("resumirDashboard",pedidosResumo,
                "buscarPedidosQuePrecisamAtencao",List.of(new DashboardPedidoAtencaoResponse(1L,"Cliente",
                        TipoEntrega.ENTREGA,StatusPedido.PENDENTE,new BigDecimal("90"),false)),
                "contarPedidosPorStatusNaData",List.of(pendentes)));
        var vendido = new ResumoProdutoVendidoResponse(1L,"Feijoada",UnidadeVenda.UNIDADE,new BigDecimal("5"),new BigDecimal("250"));
        ItemPedidoRepository itens = proxy(ItemPedidoRepository.class,Map.of("resumirProdutosMaisVendidosDashboard",List.of(vendido)));
        ProducaoRepository.ResumoDashboard producaoResumo = proxy(ProducaoRepository.ResumoDashboard.class,
                Map.of("getId",7L,"getStatus",StatusProducao.CONFIRMADA,"getProdutos",2L,
                        "getQuantidade",new BigDecimal("35"),"getCusto",new BigDecimal("312.40"),
                        "getConfirmadaEm",LocalDateTime.of(2026,8,3,10,0)));
        ProducaoRepository producoes = proxy(ProducaoRepository.class,Map.of("resumirDashboard",Optional.of(producaoResumo)));
        SaldoEstoqueRepository.ResumoDashboard estoqueResumo = proxy(SaldoEstoqueRepository.ResumoDashboard.class,
                Map.of("getItensComSaldo",8L,"getAbaixoDoMinimo",2L,"getSemEstoque",1L,
                        "getProduzidosDisponiveis",2L,"getRevendaDisponiveis",1L,"getValorInsumos",new BigDecimal("500"),
                        "getValorRevenda",new BigDecimal("100"),"getValorProduzidos",new BigDecimal("300"),"getValorTotal",new BigDecimal("900")));
        SaldoEstoqueRepository.Visao feijao = visao("INSUMO",1L,"Feijão","QUILOGRAMA",BigDecimal.ZERO,new BigDecimal("2"));
        SaldoEstoqueRepository.Visao feijoada = visao("PREPARACAO_PRODUZIDA",2L,"Feijoada","UNIDADE",new BigDecimal("20"),BigDecimal.ZERO);
        SaldoEstoqueRepository estoque = proxy(SaldoEstoqueRepository.class,Map.of("resumirDashboard",estoqueResumo,
                "listarAlertasDashboard",List.of(feijao),"listarProduzidosDisponiveisDashboard",List.of(feijoada)));
        CompraRepository.ResumoDashboard compraResumo = proxy(CompraRepository.ResumoDashboard.class,
                Map.of("getQuantidade",2L,"getValorTotal",new BigDecimal("200"),"getComprasInsumos",1L,
                        "getValorInsumos",new BigDecimal("150"),"getComprasRevenda",1L,"getValorRevenda",new BigDecimal("50")));
        CompraRepository compras = proxy(CompraRepository.class,Map.of("resumirDashboard",compraResumo));
        FichaTecnicaRepository fichas = proxy(FichaTecnicaRepository.class,
                Map.of("contarAtivasComCustoPendente",2L,"contarProdutosProduzidosAtivosSemFicha",1L));

        DashboardOperacionalResponse d = new DashboardService(pedidos,itens,producoes,estoque,compras,fichas).buscarDashboard(data);

        assertThat(d.pedidos().faturamentoDoDia()).isEqualByComparingTo("480");
        assertThat(d.pedidosAtencao()).hasSize(1);
        assertThat(d.producao().custoReal()).isEqualByComparingTo("312.40");
        assertThat(d.estoque().produtosProduzidos()).extracting(DashboardOperacionalResponse.ItemEstoque::nome).containsExactly("Feijoada");
        assertThat(d.compras().valorInsumos()).isEqualByComparingTo("150");
        assertThat(d.alertas()).extracting(DashboardOperacionalResponse.Alerta::titulo)
                .containsExactly("Itens sem estoque","Estoque abaixo do mínimo","Custo de ficha pendente","Produtos sem Ficha Técnica");
    }

    private SaldoEstoqueRepository.Visao visao(String tipo,Long id,String nome,String unidade,BigDecimal saldo,BigDecimal minimo) {
        return proxy(SaldoEstoqueRepository.Visao.class,Map.of("getTipoItem",tipo,"getReferenciaId",id,
                "getItemNome",nome,"getUnidade",unidade,"getAtivo",true,"getQuantidadeAtual",saldo,
                "getEstoqueMinimo",minimo,"getCustoMedioAtual",BigDecimal.ZERO,"getValorTotalEstoque",BigDecimal.ZERO));
    }

    @Test
    void deveMontarResumoComUnidadesQuilosSubtotaisHistoricosETaxas() {
        LocalDate data = LocalDate.of(2026, 8, 3);
        List<ResumoProdutoVendidoResponse> produtos = List.of(
                new ResumoProdutoVendidoResponse(
                        1L,
                        "Frango Assado",
                        UnidadeVenda.UNIDADE,
                        new BigDecimal("3.000"),
                        new BigDecimal("150.00")
                ),
                new ResumoProdutoVendidoResponse(
                        2L,
                        "Joelho de Porco",
                        UnidadeVenda.QUILOGRAMA,
                        new BigDecimal("4.250"),
                        new BigDecimal("220.00")
                )
        );
        ItemPedidoRepository itemPedidoRepository = proxy(
                ItemPedidoRepository.class,
                Map.of("resumirProdutosVendidosPorDataExcetoStatus", produtos)
        );
        PedidoRepository pedidoRepository = proxy(
                PedidoRepository.class,
                Map.of("somarTaxasEntregaPorDataExcetoStatus", new BigDecimal("35.00"))
        );
        DashboardService dashboardService = new DashboardService(
                pedidoRepository,
                itemPedidoRepository,
                null, null, null, null
        );

        ResumoVendasDiaResponse resumo = dashboardService.buscarResumoVendasDia(data);

        assertThat(resumo.dataReferencia()).isEqualTo(data);
        assertThat(resumo.produtos()).containsExactlyElementsOf(produtos);
        assertThat(resumo.produtos().get(0).quantidadeTotal()).isEqualByComparingTo("3.000");
        assertThat(resumo.produtos().get(1).quantidadeTotal()).isEqualByComparingTo("4.250");
        assertThat(resumo.totalProdutos()).isEqualByComparingTo("370.00");
        assertThat(resumo.totalTaxasEntrega()).isEqualByComparingTo("35.00");
        assertThat(resumo.totalGeral()).isEqualByComparingTo("405.00");

        FormatacaoUtil formatacao = new FormatacaoUtil();
        assertThat(formatacao.formatarQuantidade(
                resumo.produtos().get(0).quantidadeTotal(),
                UnidadeVenda.UNIDADE
        )).isEqualTo("3");
        assertThat(formatacao.formatarQuantidade(
                resumo.produtos().get(1).quantidadeTotal(),
                UnidadeVenda.QUILOGRAMA
        )).isEqualTo("4,250 kg");
    }

    @Test
    void deveRetornarTotaisZeradosQuandoNaoHouverVendas() {
        LocalDate data = LocalDate.of(2026, 8, 4);
        ItemPedidoRepository itemPedidoRepository = proxy(
                ItemPedidoRepository.class,
                Map.of("resumirProdutosVendidosPorDataExcetoStatus", List.of())
        );
        PedidoRepository pedidoRepository = proxy(PedidoRepository.class, Map.of());
        DashboardService dashboardService = new DashboardService(
                pedidoRepository,
                itemPedidoRepository,
                null, null, null, null
        );

        ResumoVendasDiaResponse resumo = dashboardService.buscarResumoVendasDia(data);

        assertThat(resumo.produtos()).isEmpty();
        assertThat(resumo.totalProdutos()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumo.totalTaxasEntrega()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumo.totalGeral()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    private <T> T proxy(Class<T> tipo, Map<String, Object> retornos) {
        return tipo.cast(Proxy.newProxyInstance(
                tipo.getClassLoader(),
                new Class<?>[]{tipo},
                (proxy, metodo, argumentos) -> retornos.get(metodo.getName())
        ));
    }
}
