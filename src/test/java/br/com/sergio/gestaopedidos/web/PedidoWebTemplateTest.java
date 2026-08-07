package br.com.sergio.gestaopedidos.web;

import br.com.sergio.gestaopedidos.controller.web.PedidoWebController;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.entity.ItemPedido;
import br.com.sergio.gestaopedidos.entity.Pedido;
import br.com.sergio.gestaopedidos.service.ClienteService;
import br.com.sergio.gestaopedidos.service.DashboardService;
import br.com.sergio.gestaopedidos.service.EstoqueService;
import br.com.sergio.gestaopedidos.service.PedidoService;
import br.com.sergio.gestaopedidos.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PedidoWebTemplateTest {

    private static final Path FORMULARIO = Path.of("src/main/resources/templates/pedidos/formulario.html");
    private static final Path DETALHES = Path.of("src/main/resources/templates/pedidos/fragments/detalhes.html");
    private static final Path COMANDA = Path.of("src/main/resources/templates/pedidos/comandas.html");

    @Test
    void novoPedidoAbreComDataAtualDefinidaPeloController() {
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller(mock(PedidoService.class)).novo(model);

        assertThat(view).isEqualTo("pedidos/formulario");
        assertThat(((PedidoRequest) model.get("pedido")).dataAgendada()).isEqualTo(LocalDate.now());
    }

    @Test
    void editarPedidoPreservaDataSalva() {
        PedidoService pedidoService = mock(PedidoService.class);
        LocalDate dataSalva = LocalDate.of(2026, 9, 18);
        when(pedidoService.buscarParaEdicao(7L)).thenReturn(PedidoResponse.builder()
                .id(7L).dataAgendada(dataSalva).itens(List.of()).build());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller(pedidoService).editar(
                7L, "lista", null, false, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("pedidos/formulario");
        assertThat(((PedidoRequest) model.get("pedido")).dataAgendada()).isEqualTo(dataSalva);
    }

    @Test
    void formularioUsaCalendarioNativoSemMascaraOuCustoHistoricoIndividual() throws Exception {
        String html = Files.readString(FORMULARIO);

        assertThat(html).contains("type=\"date\" id=\"dataAgendada\"", "th:field=\"*{dataAgendada}\"");
        assertThat(html).doesNotContain("dataAgendadaVisual", "data-data-brasileira", "Custo unitário histórico");
    }

    @Test
    void detalhesAdminMostramHistoricoOuAvisoSemApresentarZeroComoConfirmado() throws Exception {
        String html = Files.readString(DETALHES);

        assertThat(html).contains("sec:authorize=\"hasRole('ADMIN')\"",
                "item.custoUnitarioHistorico", "item.custoTotalHistorico",
                "item.lucroBrutoEstimado", "item.margemBrutaHistorica",
                "Custo histórico ainda não disponível",
                "Será confirmado ao iniciar a preparação");
        assertThat(html).contains("pedido.custoTotalHistorico == null");
    }

    @Test
    void custoHistoricoDeItemPorUnidadeCalculaLucroEMargem() {
        ItemPedido item = ItemPedido.builder()
                .quantidade(new BigDecimal("1.000"))
                .subtotal(new BigDecimal("50.00"))
                .build();

        item.aplicarCustoHistorico(new BigDecimal("19.00"));

        assertThat(item.getCustoUnitarioHistorico()).isEqualByComparingTo("19.000000");
        assertThat(item.getCustoTotalHistorico()).isEqualByComparingTo("19.00");
        assertThat(item.getLucroBrutoHistorico()).isEqualByComparingTo("31.00");
        assertThat(item.getMargemBrutaHistorica()).isEqualByComparingTo("62.0000");
    }

    @Test
    void custoHistoricoDeItemPorKgPreservaQuantidadeDecimal() {
        ItemPedido item = ItemPedido.builder()
                .quantidade(new BigDecimal("1.235"))
                .subtotal(new BigDecimal("67.93"))
                .build();

        item.aplicarCustoHistorico(new BigDecimal("32.11"));

        assertThat(item.getQuantidade()).isEqualByComparingTo("1.235");
        assertThat(item.getCustoTotalHistorico()).isEqualByComparingTo("32.11");
        assertThat(item.getLucroBrutoHistorico()).isEqualByComparingTo("35.82");
        assertThat(item.getMargemBrutaHistorica()).isEqualByComparingTo("52.7308");
    }

    @Test
    void resumoHistoricoExcluiTaxaDeEntregaDoLucro() {
        Pedido pedido = Pedido.builder()
                .subtotal(new BigDecimal("100.00"))
                .taxaEntrega(new BigDecimal("15.00"))
                .valorTotal(new BigDecimal("115.00"))
                .build();

        pedido.aplicarResultadoHistorico(new BigDecimal("40.00"));

        assertThat(pedido.getCustoTotalHistorico()).isEqualByComparingTo("40.00");
        assertThat(pedido.getLucroBrutoEstimado()).isEqualByComparingTo("60.00");
        assertThat(pedido.getMargemBrutaEstimada()).isEqualByComparingTo("60.0000");
    }

    @Test
    void impressaoNaoMostraCustosInternos() throws Exception {
        String html = Files.readString(COMANDA);

        assertThat(html).doesNotContain("custoUnitarioHistorico", "custoTotalHistorico",
                "lucroBrutoEstimado", "margemBrutaHistorica", "CMV");
    }

    private PedidoWebController controller(PedidoService pedidoService) {
        return new PedidoWebController(
                pedidoService,
                mock(EstoqueService.class),
                mock(DashboardService.class),
                mock(ClienteService.class),
                mock(ProdutoService.class)
        );
    }
}
