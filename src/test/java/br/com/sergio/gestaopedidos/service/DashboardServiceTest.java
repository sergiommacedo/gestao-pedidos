package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.resumo.ResumoProdutoVendidoResponse;
import br.com.sergio.gestaopedidos.dto.resumo.ResumoVendasDiaResponse;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.repository.ItemPedidoRepository;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import br.com.sergio.gestaopedidos.util.FormatacaoUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceTest {

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
                itemPedidoRepository
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
                itemPedidoRepository
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
