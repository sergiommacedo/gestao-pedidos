package br.com.sergio.gestaopedidos.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardTemplateTest {
    private final Path template = Path.of("src/main/resources/templates/dashboard/dashboard.html");
    private final Path modal = Path.of("src/main/resources/templates/dashboard/fragments/modal-historico-cliente.html");
    private final Path historico = Path.of("src/main/resources/templates/dashboard/fragments/historico-cliente.html");
    private final Path itens = Path.of("src/main/resources/templates/dashboard/fragments/itens-historicos.html");
    private final Path javascript = Path.of("src/main/resources/static/js/app.js");

    @Test void mostraLucroHistoricoOuAvisoQuandoExistemPedidosSemCusto() throws Exception {
        String html = Files.readString(template);
        assertThat(html).contains("Lucro bruto estimado", "dashboard.resultadoFinanceiro.lucroBruto",
                "dashboard.resultadoFinanceiro.cmv", "dashboard.resultadoFinanceiro.margemBruta",
                "Existem pedidos ainda sem custo confirmado.");
        assertThat(html).doesNotContain("formatarMoeda(analitico.lucroBrutoEstimado)");
    }

    @Test void ocultaAreaAnaliticaEBlocosIndividuaisQuandoNaoHaDados() throws Exception {
        String html = Files.readString(template);
        assertThat(html).contains("th:if=\"${!#lists.isEmpty(analitico.vendasPorDia) or !#lists.isEmpty(analitico.producaoPorDia)");
        assertThat(html).contains("th:if=\"${!#lists.isEmpty(analitico.vendasPorDia)}\"");
        assertThat(html).contains("th:if=\"${!#lists.isEmpty(analitico.producaoPorDia)}\"");
    }

    @Test void distingueProducaoConfirmadaDeRascunho() throws Exception {
        String html = Files.readString(template);
        assertThat(html).contains("Produção confirmada hoje", "Produções em rascunho",
                "ainda não fazem parte do Estoque", "Confirmar Produção");
        assertThat(html).doesNotContain("Produtos fabricados hoje");
    }

    @Test void mostraUltimasComprasCompactasEEstadoVazio() throws Exception {
        String html = Files.readString(template);
        assertThat(html).contains("Últimas compras", "dashboard.compras", "compra.quantidadeItens",
                "compra.valorTotal", "compra.classificacao", "Ver Compras", "Nenhuma compra cadastrada.");
        assertThat(html).doesNotContain("Compras do dia", "Nenhuma compra registrada hoje.");
    }

    @Test void rankingClientesMostraQuantidadeTotalETicketMedio() throws Exception {
        String html = Files.readString(template);
        assertThat(html).contains("Ranking de clientes", "r.quantidadePedidos", "r.valorTotal",
                "Ticket médio:", "r.ticketMedio");
        assertThat(html).doesNotContain("formatarMoeda(r.valor)");
    }

    @Test void rankingAbreModalEHistoricoSoEhConsultadoAposClique() throws Exception {
        String html = Files.readString(template);
        assertThat(html).contains("Ver pedidos", "#modalHistoricoCliente", "data-historico-cliente-url",
                "/dashboard/clientes/{id}/pedidos", "modal-historico-cliente");
        assertThat(html).doesNotContain("historico.pedidos");
        assertThat(Files.readString(modal)).contains("data-conteudo-historico-cliente", "Carregando pedidos");
        assertThat(Files.readString(javascript)).contains("show.bs.modal", "dataset.historicoClienteUrl",
                "data-itens-historicos-url", "dataset.historicoPaginaUrl", "fetch(");
    }

    @Test void modalMostraPedidosPaginacaoItensETotaisHistoricos() throws Exception {
        String pagina = Files.readString(historico);
        String detalhe = Files.readString(itens);
        assertThat(pagina).contains("Pedidos de ", "historico.quantidadeTotal", "historico.valorTotal",
                "historico.ticketMedio", "pedido.data", "pedido.tipoEntrega.descricao", "pedido.status.descricao",
                "pedido.subtotal", "pedido.taxaEntrega", "pedido.valorTotal", "Anterior", "Próxima", "Ver itens");
        assertThat(detalhe).contains("item.produtoNome", "formatarQuantidade(item.quantidade,item.unidade)",
                "item.precoUnitario", "item.subtotal", "Subtotal dos produtos", "Taxa de entrega", "detalhes.valorTotal");
        assertThat(pagina + detalhe).doesNotContain("custo", "lucro", "margem");
    }
}
