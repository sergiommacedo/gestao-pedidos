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
import br.com.sergio.gestaopedidos.entity.Producao;
import br.com.sergio.gestaopedidos.dto.producao.*;
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
import static org.mockito.Mockito.*;

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
                "contarPedidosPorStatusNaData", List.of(),
                "resumirFinanceiroDashboard", financeiro(0,0,"0","0","0")));
        ItemPedidoRepository itens = proxy(ItemPedidoRepository.class,
                Map.of("resumirProdutosMaisVendidosDashboard", List.of()));
        ProducaoRepository.ResumoDashboard producaoVazia = proxy(ProducaoRepository.ResumoDashboard.class,
                Map.of("getProducoes",0L,"getProdutos",0L,"getQuantidade",BigDecimal.ZERO,"getCusto",BigDecimal.ZERO));
        ProducaoRepository producoes = proxy(ProducaoRepository.class, Map.of("resumirConfirmadasDashboard", producaoVazia,
                "buscarRascunhosDashboard",List.of()));
        SaldoEstoqueRepository.ResumoDashboard estoqueResumo = proxy(SaldoEstoqueRepository.ResumoDashboard.class,
                Map.of("getItensComSaldo",0L,"getAbaixoDoMinimo",0L,"getSemEstoque",0L,
                        "getProduzidosDisponiveis",0L,"getRevendaDisponiveis",0L,"getValorInsumos",BigDecimal.ZERO,
                        "getValorRevenda",BigDecimal.ZERO,"getValorProduzidos",BigDecimal.ZERO,"getValorTotal",BigDecimal.ZERO));
        SaldoEstoqueRepository estoque = proxy(SaldoEstoqueRepository.class, Map.of(
                "resumirDashboard", estoqueResumo, "listarAlertasDashboard", List.of(),
                "listarProduzidosDisponiveisDashboard", List.of()));
        CompraRepository compras = proxy(CompraRepository.class, Map.of("buscarUltimasAtivas", List.of()));
        FichaTecnicaRepository fichas = proxy(FichaTecnicaRepository.class,
                Map.of("contarAtivasComCustoPendente",0L,"contarProdutosProduzidosAtivosSemFicha",0L));

        EstoqueService estoqueService=mock(EstoqueService.class);when(estoqueService.indicadores()).thenReturn(indicadoresEstoque("0","0","0"));
        DashboardOperacionalResponse dashboard = new DashboardService(pedidoRepository, itens, producoes,
                estoque, compras, fichas, estoqueService, mock(ProducaoService.class)).buscarDashboard(data);

        assertThat(dashboard.pedidos().agendadosHoje()).isZero();
        assertThat(dashboard.producao().existe()).isFalse();
        assertThat(dashboard.estoque().produtosProduzidos()).isEmpty();
        assertThat(dashboard.compras()).isEmpty();
        assertThat(dashboard.resultadoFinanceiro().completo()).isTrue();
        assertThat(dashboard.resultadoFinanceiro().margemBruta()).isNull();
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
                "resumirFinanceiroDashboard",financeiro(5,5,"450","280","170"),
                "buscarPedidosQuePrecisamAtencao",List.of(new DashboardPedidoAtencaoResponse(1L,"Cliente",
                        TipoEntrega.ENTREGA,StatusPedido.PENDENTE,new BigDecimal("90"),false)),
                "contarPedidosPorStatusNaData",List.of(pendentes)));
        var vendido = new ResumoProdutoVendidoResponse(1L,"Feijoada",UnidadeVenda.UNIDADE,new BigDecimal("5"),new BigDecimal("250"));
        ItemPedidoRepository itens = proxy(ItemPedidoRepository.class,Map.of("resumirProdutosMaisVendidosDashboard",List.of(vendido)));
        ProducaoRepository.ResumoDashboard producaoResumo = proxy(ProducaoRepository.ResumoDashboard.class,
                Map.of("getProducoes",2L,"getProdutos",2L,"getQuantidade",new BigDecimal("35"),"getCusto",new BigDecimal("312.40")));
        ProducaoRepository producoes = proxy(ProducaoRepository.class,Map.of("resumirConfirmadasDashboard",producaoResumo,
                "buscarRascunhosDashboard",List.of()));
        SaldoEstoqueRepository.ResumoDashboard estoqueResumo = proxy(SaldoEstoqueRepository.ResumoDashboard.class,
                Map.of("getItensComSaldo",8L,"getAbaixoDoMinimo",2L,"getSemEstoque",1L,
                        "getProduzidosDisponiveis",2L,"getRevendaDisponiveis",1L,"getValorInsumos",new BigDecimal("500"),
                        "getValorRevenda",new BigDecimal("100"),"getValorProduzidos",new BigDecimal("300"),"getValorTotal",new BigDecimal("900")));
        SaldoEstoqueRepository.Visao feijao = visao("INSUMO",1L,"Feijão","QUILOGRAMA",BigDecimal.ZERO,new BigDecimal("2"));
        SaldoEstoqueRepository.Visao feijoada = visao("PREPARACAO_PRODUZIDA",2L,"Feijoada","UNIDADE",new BigDecimal("20"),BigDecimal.ZERO);
        SaldoEstoqueRepository estoque = proxy(SaldoEstoqueRepository.class,Map.of("resumirDashboard",estoqueResumo,
                "listarAlertasDashboard",List.of(feijao),"listarProduzidosDisponiveisDashboard",List.of(feijoada)));
        CompraRepository.CompraRecente compra = proxy(CompraRepository.CompraRecente.class,
                Map.of("getId",7L,"getTipoCompra",TipoCompra.MISTA,"getDataCompra",data,
                        "getFornecedor","Rio Verde","getValorTotal",new BigDecimal("200"),"getQuantidadeItens",4L));
        CompraRepository compras = proxy(CompraRepository.class,Map.of("buscarUltimasAtivas",List.of(compra)));
        FichaTecnicaRepository fichas = proxy(FichaTecnicaRepository.class,
                Map.of("contarAtivasComCustoPendente",2L,"contarProdutosProduzidosAtivosSemFicha",1L));

        EstoqueService estoqueService=mock(EstoqueService.class);when(estoqueService.indicadores()).thenReturn(indicadoresEstoque("500","100","300"));
        DashboardOperacionalResponse d = new DashboardService(pedidos,itens,producoes,estoque,compras,fichas,
                estoqueService,mock(ProducaoService.class)).buscarDashboard(data);

        assertThat(d.pedidos().faturamentoDoDia()).isEqualByComparingTo("480");
        assertThat(d.resultadoFinanceiro().cmv()).isEqualByComparingTo("280");
        assertThat(d.resultadoFinanceiro().lucroBruto()).isEqualByComparingTo("170");
        assertThat(d.resultadoFinanceiro().margemBruta()).isEqualByComparingTo("37.78");
        assertThat(d.resultadoFinanceiro().completo()).isTrue();
        assertThat(d.pedidosAtencao()).hasSize(1);
        assertThat(d.producao().custoReal()).isEqualByComparingTo("312.40");
        assertThat(d.producao().quantidadeProducoes()).isEqualTo(2);
        assertThat(d.estoque().valorTotal()).isEqualByComparingTo("900");
        assertThat(d.estoque().produtosProduzidos()).extracting(DashboardOperacionalResponse.ItemEstoque::nome).containsExactly("Feijoada");
        assertThat(d.compras()).singleElement().satisfies(c -> {
            assertThat(c.fornecedor()).isEqualTo("Rio Verde");
            assertThat(c.quantidadeItens()).isEqualTo(4);
            assertThat(c.valorTotal()).isEqualByComparingTo("200");
            assertThat(c.classificacao()).isEqualTo("Mista");
        });
        assertThat(d.alertas()).extracting(DashboardOperacionalResponse.Alerta::titulo)
                .containsExactly("Itens sem estoque","Estoque abaixo do mínimo","Custo de ficha pendente","Produtos sem Ficha Técnica");
    }

    @Test void rascunhoApareceComoPendenciaSemEntrarNosTotaisConfirmados(){
        LocalDate data=LocalDate.of(2026,8,4);PedidoRepository pedidos=mock(PedidoRepository.class);ItemPedidoRepository itens=mock(ItemPedidoRepository.class);ProducaoRepository producoes=mock(ProducaoRepository.class);SaldoEstoqueRepository saldos=mock(SaldoEstoqueRepository.class);CompraRepository compras=mock(CompraRepository.class);FichaTecnicaRepository fichas=mock(FichaTecnicaRepository.class);EstoqueService estoqueService=mock(EstoqueService.class);ProducaoService producaoService=mock(ProducaoService.class);
        PedidoRepository.ResumoDashboard pr=mock(PedidoRepository.ResumoDashboard.class);when(pedidos.resumirDashboard(any(),any(),any(),any())).thenReturn(pr);when(pedidos.buscarPedidosQuePrecisamAtencao(any(),any(),any(),any(),any(),any(),any())).thenReturn(List.of());when(pedidos.contarPedidosPorStatusNaData(data)).thenReturn(List.of());when(itens.resumirProdutosMaisVendidosDashboard(any(),any(),any())).thenReturn(List.of());
        ProducaoRepository.ResumoDashboard confirmadas=mock(ProducaoRepository.ResumoDashboard.class);when(producoes.resumirConfirmadasDashboard(data)).thenReturn(confirmadas);Producao rascunho=Producao.builder().id(9L).dataProducao(data).status(StatusProducao.RASCUNHO).build();when(producoes.buscarRascunhosDashboard(data)).thenReturn(List.of(rascunho));
        var estoquePreparacao=EstoquePreparacaoProducaoResponse.builder().produtoNome("Feijoada Pronta").unidade(UnidadeMedida.QUILOGRAMA).producaoAdicionada(new BigDecimal("35.000")).estoqueAntes(new BigDecimal("0.000")).build();var resposta=ProducaoResponse.builder().custoTotal(new BigDecimal("313.90")).build();when(producaoService.buscarDetalhes(9L)).thenReturn(ProducaoDetalhesResponse.builder().resumo(new ProducaoResumoResponse(resposta,new BigDecimal("313.90"))).estoquesPreparacoes(List.of(estoquePreparacao)).build());
        SaldoEstoqueRepository.ResumoDashboard sr=mock(SaldoEstoqueRepository.ResumoDashboard.class);when(saldos.resumirDashboard()).thenReturn(sr);when(saldos.listarAlertasDashboard(any())).thenReturn(List.of());when(saldos.listarProduzidosDisponiveisDashboard(any())).thenReturn(List.of());when(estoqueService.indicadores()).thenReturn(indicadoresEstoque("1481","675","0"));when(compras.buscarUltimasAtivas(any())).thenReturn(List.of());
        var d=new DashboardService(pedidos,itens,producoes,saldos,compras,fichas,estoqueService,producaoService).buscarDashboard(data);
        assertThat(d.producao().quantidadeProducoes()).isZero();assertThat(d.producao().quantidadeTotal()).isEqualByComparingTo("0");assertThat(d.producao().custoReal()).isEqualByComparingTo("0");assertThat(d.producoesRascunho()).singleElement().satisfies(r->{assertThat(r.custoEstimado()).isEqualByComparingTo("313.90");assertThat(r.preparacoes().getFirst().estoqueAtual()).isEqualByComparingTo("0.000");});assertThat(d.alertas()).extracting(DashboardOperacionalResponse.Alerta::titulo).contains("Produção em rascunho");assertThat(d.estoque().valorTotal()).isEqualByComparingTo("2156");
    }

    @Test void resultadoFinanceiroFicaIndisponivelQuandoExistePedidoSemCustoConfirmado(){
        LocalDate data=LocalDate.of(2026,8,4);
        PedidoRepository pedidos=proxy(PedidoRepository.class,Map.of(
                "resumirFinanceiroDashboard",financeiro(2,1,"167.93","51.11","116.82")));
        var resultado=new DashboardService(pedidos,null,null,null,null,null,null,null)
                .buscarResultadoFinanceiro(data);
        assertThat(resultado.completo()).isFalse();
        assertThat(resultado.pedidosSemCusto()).isEqualTo(1);
        assertThat(resultado.cmv()).isEqualByComparingTo("51.11");
        assertThat(resultado.lucroBruto()).isEqualByComparingTo("116.82");
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
                null, null, null, null, null, null
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
                null, null, null, null, null, null
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

    private br.com.sergio.gestaopedidos.dto.estoque.EstoqueIndicadoresResponse indicadoresEstoque(String insumos,String revenda,String produzidos){BigDecimal a=new BigDecimal(insumos),b=new BigDecimal(revenda),c=new BigDecimal(produzidos);return br.com.sergio.gestaopedidos.dto.estoque.EstoqueIndicadoresResponse.builder().valorInsumos(a).valorRevenda(b).valorProduzidos(c).valorTotal(a.add(b).add(c)).build();}
    private PedidoRepository.ResumoFinanceiroDashboard financeiro(long validos,long comCusto,String receita,String cmv,String lucro){return proxy(PedidoRepository.ResumoFinanceiroDashboard.class,Map.of("getPedidosValidos",validos,"getPedidosComCusto",comCusto,"getReceitaProdutos",new BigDecimal(receita),"getCmv",new BigDecimal(cmv),"getLucroBruto",new BigDecimal(lucro)));}
}
