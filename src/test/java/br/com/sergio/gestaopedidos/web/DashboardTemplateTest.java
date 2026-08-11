package br.com.sergio.gestaopedidos.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardTemplateTest {
    private final Path template = Path.of("src/main/resources/templates/dashboard/dashboard.html");

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
}
