package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoLinhaResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelatorioPedidoServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 8, 31);

    @Test
    void deveCalcularPeriodoComPedidosValidosECanceladosETicketMedio() {
        RepositorioFake fake = new RepositorioFake(
                Page.empty(),
                indicadores(3L, 2L, 1L, "201.01", "20.00")
        );
        RelatorioPedidoService service = new RelatorioPedidoService(fake.repository());

        var resultado = service.buscar(filtroPadrao(), PageRequest.of(0, 10));

        assertThat(resultado.indicadores().totalPedidos()).isEqualTo(3);
        assertThat(resultado.indicadores().pedidosValidos()).isEqualTo(2);
        assertThat(resultado.indicadores().cancelados()).isEqualTo(1);
        assertThat(resultado.indicadores().faturamento()).isEqualByComparingTo("201.01");
        assertThat(resultado.indicadores().taxasEntrega()).isEqualByComparingTo("20.00");
        assertThat(resultado.indicadores().ticketMedio()).isEqualByComparingTo("100.51");
    }

    @Test
    void deveZerarValoresETicketQuandoHouverSomenteCancelados() {
        RepositorioFake fake = new RepositorioFake(
                Page.empty(),
                indicadores(2L, 0L, 2L, "0.00", "0.00")
        );

        var indicadores = new RelatorioPedidoService(fake.repository())
                .buscar(filtroPadrao(), PageRequest.of(0, 10))
                .indicadores();

        assertThat(indicadores.totalPedidos()).isEqualTo(2);
        assertThat(indicadores.cancelados()).isEqualTo(2);
        assertThat(indicadores.faturamento()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(indicadores.taxasEntrega()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(indicadores.ticketMedio()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deveRetornarPaginaEIndicadoresVaziosQuandoNaoHouverPedidos() {
        RepositorioFake fake = new RepositorioFake(Page.empty(), indicadores(0L, 0L, 0L, "0", "0"));

        var resultado = new RelatorioPedidoService(fake.repository())
                .buscar(filtroPadrao(), PageRequest.of(0, 10));

        assertThat(resultado.pedidos()).isEmpty();
        assertThat(resultado.indicadores().totalPedidos()).isZero();
        assertThat(resultado.indicadores().ticketMedio()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deveEncaminharFiltroPorCliente() {
        RepositorioFake fake = fakeVazio();
        var filtro = new RelatorioPedidoService.FiltroRelatorioPedidos(
                INICIO, FIM, "  Maria  ", null, null, null
        );

        new RelatorioPedidoService(fake.repository()).buscar(filtro, PageRequest.of(0, 10));

        assertThat(fake.argumentosPagina[2]).isEqualTo("Maria");
        assertThat(fake.argumentosIndicadores[2]).isEqualTo("Maria");
    }

    @Test
    void deveEncaminharFiltroPorStatus() {
        RepositorioFake fake = fakeVazio();
        var filtro = new RelatorioPedidoService.FiltroRelatorioPedidos(
                INICIO, FIM, "", StatusPedido.ENTREGUE, null, null
        );

        new RelatorioPedidoService(fake.repository()).buscar(filtro, PageRequest.of(0, 10));

        assertThat(fake.argumentosPagina[3]).isEqualTo(StatusPedido.ENTREGUE);
        assertThat(fake.argumentosIndicadores[3]).isEqualTo(StatusPedido.ENTREGUE);
    }

    @Test
    void deveEncaminharFiltroPorEntrega() {
        RepositorioFake fake = fakeVazio();
        var filtro = new RelatorioPedidoService.FiltroRelatorioPedidos(
                INICIO, FIM, "", null, TipoEntrega.RETIRADA, null
        );

        new RelatorioPedidoService(fake.repository()).buscar(filtro, PageRequest.of(0, 10));

        assertThat(fake.argumentosPagina[4]).isEqualTo(TipoEntrega.RETIRADA);
        assertThat(fake.argumentosIndicadores[4]).isEqualTo(TipoEntrega.RETIRADA);
    }

    @Test
    void deveEncaminharFiltroPorPagamento() {
        RepositorioFake fake = fakeVazio();
        var filtro = new RelatorioPedidoService.FiltroRelatorioPedidos(
                INICIO, FIM, "", null, null, FormaPagamento.PIX
        );

        new RelatorioPedidoService(fake.repository()).buscar(filtro, PageRequest.of(0, 10));

        assertThat(fake.argumentosPagina[5]).isEqualTo(FormaPagamento.PIX);
        assertThat(fake.argumentosIndicadores[5]).isEqualTo(FormaPagamento.PIX);
    }

    @Test
    void deveRejeitarPeriodoInvalidoSemConsultarRepository() {
        RepositorioFake fake = fakeVazio();
        var filtro = new RelatorioPedidoService.FiltroRelatorioPedidos(
                FIM, INICIO, "", null, null, null
        );

        assertThatThrownBy(() -> new RelatorioPedidoService(fake.repository())
                .buscar(filtro, PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A data inicial não pode ser posterior à data final.");
        assertThat(fake.argumentosPagina).isNull();
        assertThat(fake.argumentosIndicadores).isNull();
    }

    @Test
    void devePreservarPaginacaoRetornadaPeloRepository() {
        PageRequest pageable = PageRequest.of(2, 20);
        Page<RelatorioPedidoLinhaResponse> pagina = new PageImpl<>(List.of(), pageable, 45);
        RepositorioFake fake = new RepositorioFake(pagina, indicadores(45L, 45L, 0L, "4500", "0"));

        var resultado = new RelatorioPedidoService(fake.repository()).buscar(filtroPadrao(), pageable);

        assertThat(resultado.pedidos().getNumber()).isEqualTo(2);
        assertThat(resultado.pedidos().getSize()).isEqualTo(20);
        assertThat(resultado.pedidos().getTotalElements()).isEqualTo(45);
        assertThat(fake.argumentosPagina[6]).isSameAs(pageable);
    }

    private RelatorioPedidoService.FiltroRelatorioPedidos filtroPadrao() {
        return new RelatorioPedidoService.FiltroRelatorioPedidos(
                INICIO, FIM, "", null, null, null
        );
    }

    private RepositorioFake fakeVazio() {
        return new RepositorioFake(Page.empty(), indicadores(0L, 0L, 0L, "0", "0"));
    }

    private PedidoRepository.IndicadoresRelatorioPedidos indicadores(
            Long total,
            Long validos,
            Long cancelados,
            String faturamento,
            String taxas
    ) {
        return (PedidoRepository.IndicadoresRelatorioPedidos) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{PedidoRepository.IndicadoresRelatorioPedidos.class},
                (proxy, metodo, argumentos) -> switch (metodo.getName()) {
                    case "getTotalPedidos" -> total;
                    case "getPedidosValidos" -> validos;
                    case "getCancelados" -> cancelados;
                    case "getFaturamento" -> new BigDecimal(faturamento);
                    case "getTaxasEntrega" -> new BigDecimal(taxas);
                    default -> null;
                }
        );
    }

    private static class RepositorioFake implements InvocationHandler {

        private final Page<RelatorioPedidoLinhaResponse> pagina;
        private final PedidoRepository.IndicadoresRelatorioPedidos indicadores;
        private Object[] argumentosPagina;
        private Object[] argumentosIndicadores;

        private RepositorioFake(
                Page<RelatorioPedidoLinhaResponse> pagina,
                PedidoRepository.IndicadoresRelatorioPedidos indicadores
        ) {
            this.pagina = pagina;
            this.indicadores = indicadores;
        }

        private PedidoRepository repository() {
            return (PedidoRepository) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{PedidoRepository.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method metodo, Object[] argumentos) {
            if ("buscarRelatorioPedidos".equals(metodo.getName())) {
                argumentosPagina = argumentos;
                return pagina;
            }
            if ("buscarIndicadoresRelatorioPedidos".equals(metodo.getName())) {
                argumentosIndicadores = argumentos;
                return indicadores;
            }
            return null;
        }
    }
}
