package br.com.sergio.gestaopedidos.web;

import br.com.sergio.gestaopedidos.controller.web.PedidoWebController;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.dto.pedido.ItemPedidoRequest;
import br.com.sergio.gestaopedidos.entity.ItemPedido;
import br.com.sergio.gestaopedidos.entity.Pedido;
import br.com.sergio.gestaopedidos.entity.Produto;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.TipoProduto;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.service.ClienteService;
import br.com.sergio.gestaopedidos.service.DashboardService;
import br.com.sergio.gestaopedidos.service.EstoqueService;
import br.com.sergio.gestaopedidos.service.PedidoService;
import br.com.sergio.gestaopedidos.service.ProdutoService;
import br.com.sergio.gestaopedidos.service.ConfiguracaoEmpresaService;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
                .id(7L).dataAgendada(dataSalva).horarioInicio(LocalTime.of(12, 30))
                .horarioFim(LocalTime.of(13, 30)).itens(List.of()).build());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller(pedidoService).editar(
                7L, "lista", null, false, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("pedidos/formulario");
        assertThat(((PedidoRequest) model.get("pedido")).dataAgendada()).isEqualTo(dataSalva);
        assertThat(((PedidoRequest) model.get("pedido")).horarioInicio()).isEqualTo("12:30");
        assertThat(((PedidoRequest) model.get("pedido")).horarioFim()).isEqualTo("13:30");
    }

    @Test
    void formularioUsaCalendarioNativoSemMascaraOuCustoHistoricoIndividual() throws Exception {
        String html = Files.readString(FORMULARIO);

        assertThat(html).contains("type=\"date\" id=\"dataAgendada\"", "th:field=\"*{dataAgendada}\"");
        assertThat(html).doesNotContain("dataAgendadaVisual", "data-data-brasileira", "Custo unitário histórico");
    }

    @Test
    void dataAgendadaDeclaraFormatoIsoExigidoPeloInputDate() throws Exception {
        DateTimeFormat formato = PedidoRequest.class.getDeclaredMethod("dataAgendada")
                .getAnnotation(DateTimeFormat.class);

        assertThat(formato).isNotNull();
        assertThat(formato.iso()).isEqualTo(DateTimeFormat.ISO.DATE);
        assertThat(LocalDate.of(2026, 8, 7).toString()).isEqualTo("2026-08-07");
    }

    @Test
    void submitPreservaDataEscolhida() {
        PedidoService pedidoService = mock(PedidoService.class);
        LocalDate escolhida = LocalDate.now().plusDays(5);
        PedidoRequest request = requestValido(escolhida);
        when(pedidoService.salvar(request)).thenReturn(PedidoResponse.builder().id(11L).build());

        String view = controller(pedidoService).salvar(
                request,
                new BeanPropertyBindingResult(request, "pedido"),
                "salvar",
                List.of(""),
                new ExtendedModelMap(),
                new RedirectAttributesModelMap()
        );

        assertThat(view).isEqualTo("redirect:/pedidos");
        verify(pedidoService).salvar(request);
        assertThat(request.dataAgendada()).isEqualTo(escolhida);
    }

    @Test
    void detalhesAdminMostramHistoricoOuAvisoSemApresentarZeroComoConfirmado() throws Exception {
        String html = Files.readString(DETALHES);

        assertThat(html).contains("sec:authorize=\"hasRole('ADMIN')\"",
                "item.custoUnitarioHistorico", "item.custoTotalHistorico",
                "item.lucroBrutoEstimado", "item.margemBrutaHistorica",
                "Custo ainda não confirmado",
                "Custo histórico ainda não disponível",
                "Será confirmado ao marcar o pedido como pronto");
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
    void produtoComercialMantemFotografiaFinanceiraDoItem() {
        Produto produtoComercial = Produto.builder()
                .nome("Feijoada Grande")
                .tipoProduto(TipoProduto.PRODUTO_COMERCIAL)
                .unidadeVenda(UnidadeVenda.UNIDADE)
                .build();
        ItemPedido item = ItemPedido.builder()
                .produto(produtoComercial)
                .quantidade(new BigDecimal("1.000"))
                .subtotal(new BigDecimal("80.00"))
                .build();

        item.aplicarCustoHistorico(new BigDecimal("31.40"));

        assertThat(item.getProduto().getTipoProduto()).isEqualTo(TipoProduto.PRODUTO_COMERCIAL);
        assertThat(item.getCustoTotalHistorico()).isEqualByComparingTo("31.40");
        assertThat(item.getLucroBrutoHistorico()).isEqualByComparingTo("48.60");
    }

    @Test
    void somaDosResultadosDosItensBateComResumoDoPedido() {
        ItemPedido primeiro = itemComHistorico("1.235", "67.93", "32.11");
        ItemPedido segundo = itemComHistorico("1.000", "50.00", "19.00");
        BigDecimal custoItens = primeiro.getCustoTotalHistorico().add(segundo.getCustoTotalHistorico());
        BigDecimal lucroItens = primeiro.getLucroBrutoHistorico().add(segundo.getLucroBrutoHistorico());
        Pedido pedido = Pedido.builder()
                .subtotal(new BigDecimal("117.93"))
                .taxaEntrega(new BigDecimal("10.00"))
                .valorTotal(new BigDecimal("127.93"))
                .build();

        pedido.aplicarResultadoHistorico(custoItens);

        assertThat(pedido.getCustoTotalHistorico()).isEqualByComparingTo(custoItens);
        assertThat(pedido.getLucroBrutoEstimado()).isEqualByComparingTo(lucroItens);
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

    @Test
    void comandaEntregaUsaSnapshotEImprimeJanelaSemConsultarClienteAtual() throws Exception {
        String html = Files.readString(COMANDA);

        assertThat(html).contains("pedido.enderecoEntrega", "pedido.numeroEntrega",
                "pedido.bairroEntrega", "pedido.cidadeEntrega", "pedido.cepEntrega",
                "pedido.complementoEntrega", "pedido.horarioInicio", "pedido.horarioFim",
                "Horário previsto:", "CEP ");
        assertThat(html).doesNotContain("pedido.cliente.endereco", "pedido.cliente.bairro",
                "pedido.cliente.cidade", "cliente.endereco");
    }

    @Test
    void comandaRetiradaMostraHorarioCondicionalENuncaEntraNoBlocoDeEndereco() throws Exception {
        String html = Files.readString(COMANDA);

        assertThat(html).contains("Retirada prevista:",
                "th:if=\"${pedido.horarioInicio != null}\"",
                "th:if=\"${pedido.tipoEntrega.name() == 'ENTREGA'}\"");
    }

    private PedidoWebController controller(PedidoService pedidoService) {
        return new PedidoWebController(
                pedidoService,
                mock(EstoqueService.class),
                mock(DashboardService.class),
                mock(ClienteService.class),
                mock(ProdutoService.class),
                mock(ConfiguracaoEmpresaService.class)
        );
    }

    private PedidoRequest requestValido(LocalDate data) {
        return PedidoRequest.builder()
                .clienteId(1L)
                .dataAgendada(data)
                .formaPagamento(FormaPagamento.PIX)
                .tipoEntrega(TipoEntrega.RETIRADA)
                .taxaEntrega(BigDecimal.ZERO)
                .itens(List.of(ItemPedidoRequest.builder()
                        .produtoId(2L)
                        .quantidade(BigDecimal.ONE)
                        .build()))
                .build();
    }

    private ItemPedido itemComHistorico(String quantidade, String subtotal, String custo) {
        ItemPedido item = ItemPedido.builder()
                .quantidade(new BigDecimal(quantidade))
                .subtotal(new BigDecimal(subtotal))
                .build();
        item.aplicarCustoHistorico(new BigDecimal(custo));
        return item;
    }
}
