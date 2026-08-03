package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioClienteLinhaResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class RelatorioClienteServiceTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 8, 31);

    @Test
    void deveMontarClientesRecorrenciaTicketDatasEntregaRetiradaParticipacaoELider() {
        var linha = linha(2, "Maria", 3, "300", INICIO, FIM, 2, 1, "75", 1);
        Fake fake = new Fake(new SliceImpl<>(List.of(linha)), indicador(1, 3, "300", 1), lider("Maria", "300"));

        var resultado = fake.service().buscar(filtro("", null, null, null, null), PageRequest.of(0, 10));

        var cliente = resultado.linhas().getContent().getFirst();
        assertThat(cliente.ticketMedio()).isEqualByComparingTo("100.00");
        assertThat(cliente.primeiraCompra()).isEqualTo(INICIO);
        assertThat(cliente.ultimaCompra()).isEqualTo(FIM);
        assertThat(cliente.entregas()).isEqualTo(2);
        assertThat(cliente.retiradas()).isEqualTo(1);
        assertThat(cliente.participacaoPercentual()).isEqualByComparingTo("75.00");
        assertThat(cliente.posicao()).isEqualTo(1);
        assertThat(resultado.indicadores().clientesRecorrentes()).isEqualTo(1);
        assertThat(resultado.indicadores().ticketMedioGeral()).isEqualByComparingTo("100.00");
        assertThat(resultado.indicadores().clienteLiderNome()).isEqualTo("Maria");
    }

    @Test
    void deveEncaminharNomeTelefoneEntregaPagamentoMinimosEExcluirCancelados() {
        Fake fake = vazio();
        fake.service().buscar(
                filtro("11999990000", TipoEntrega.ENTREGA, FormaPagamento.PIX, 2L, new BigDecimal("150.50")),
                PageRequest.of(1, 20)
        );
        Object[] argumentos = fake.argumentosPagina.get();
        assertThat(argumentos[2]).isEqualTo("11999990000");
        assertThat(argumentos[3]).isEqualTo(TipoEntrega.ENTREGA);
        assertThat(argumentos[4]).isEqualTo(FormaPagamento.PIX);
        assertThat(argumentos[5]).isEqualTo(2L);
        assertThat(argumentos[6]).isEqualTo(new BigDecimal("150.50"));
        assertThat(argumentos[7]).isEqualTo(StatusPedido.CANCELADO);
        assertThat(((Pageable) argumentos[10]).getPageNumber()).isEqualTo(1);
    }

    @Test
    void deveRetornarPeriodoVazioComIndicadoresZeradosELiderNenhum() {
        var resultado = vazio().service().buscar(filtro("", null, null, null, null), PageRequest.of(0, 10));
        assertThat(resultado.linhas()).isEmpty();
        assertThat(resultado.indicadores().clientesCompradores()).isZero();
        assertThat(resultado.indicadores().pedidosValidos()).isZero();
        assertThat(resultado.indicadores().ticketMedioGeral()).isEqualByComparingTo("0.00");
        assertThat(resultado.indicadores().clienteLiderNome()).isEqualTo("Nenhum");
    }

    @Test
    void deveValidarPeriodoQuantidadeEValorMinimos() {
        var service = vazio().service();
        assertThatThrownBy(() -> service.buscar(
                new RelatorioClienteService.FiltroRelatorioClientes(FIM, INICIO, "", null, null, null, null),
                PageRequest.of(0, 10))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.buscar(filtro("", null, null, 0L, null), PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("quantidade mínima");
        assertThatThrownBy(() -> service.buscar(
                filtro("", null, null, null, new BigDecimal("-0.01")), PageRequest.of(0, 10)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("não pode ser negativo");
    }

    @Test
    void deveAplicarLimiteDeDezMilNaImpressaoEExcel() {
        Fake fake = vazio();
        fake.loteCheio = true;
        assertThatThrownBy(() -> fake.service().buscarParaSaida(
                filtro("", null, null, null, null), Sort.by("cliente.nome")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("10.000 clientes");
    }

    private RelatorioClienteService.FiltroRelatorioClientes filtro(
            String cliente, TipoEntrega entrega, FormaPagamento pagamento,
            Long minimoPedidos, BigDecimal minimoMovimentado
    ) {
        return new RelatorioClienteService.FiltroRelatorioClientes(
                INICIO, FIM, cliente, entrega, pagamento, minimoPedidos, minimoMovimentado
        );
    }

    private Fake vazio() { return new Fake(new SliceImpl<>(List.of()), indicador(0, 0, "0", 0), null); }

    private RelatorioClienteLinhaResponse linha(
            long id, String nome, long pedidos, String faturamento, LocalDate primeira,
            LocalDate ultima, long entregas, long retiradas, String participacao, long posicao
    ) {
        return new RelatorioClienteLinhaResponse(
                id, nome, "11999990000", pedidos, new BigDecimal(faturamento), BigDecimal.ZERO,
                primeira, ultima, entregas, retiradas, new BigDecimal(participacao), posicao
        );
    }

    private PedidoRepository.IndicadoresRelatorioClientes indicador(long clientes, long pedidos, String faturamento, long recorrentes) {
        return proxy(PedidoRepository.IndicadoresRelatorioClientes.class, Map.of(
                "getClientesCompradores", clientes, "getPedidosValidos", pedidos,
                "getFaturamentoTotal", new BigDecimal(faturamento), "getClientesRecorrentes", recorrentes
        ));
    }

    private PedidoRepository.ClienteLiderRelatorio lider(String nome, String faturamento) {
        return proxy(PedidoRepository.ClienteLiderRelatorio.class, Map.of(
                "getClienteNome", nome, "getFaturamentoTotal", new BigDecimal(faturamento)
        ));
    }

    private <T> T proxy(Class<T> tipo, Map<String, Object> valores) {
        return tipo.cast(Proxy.newProxyInstance(
                tipo.getClassLoader(), new Class[]{tipo}, (proxy, metodo, args) -> valores.get(metodo.getName())
        ));
    }

    private class Fake implements InvocationHandler {
        private final Slice<RelatorioClienteLinhaResponse> pagina;
        private final PedidoRepository.IndicadoresRelatorioClientes indicadores;
        private final PedidoRepository.ClienteLiderRelatorio lider;
        private final AtomicReference<Object[]> argumentosPagina = new AtomicReference<>();
        private boolean loteCheio;

        private Fake(Slice<RelatorioClienteLinhaResponse> pagina,
                     PedidoRepository.IndicadoresRelatorioClientes indicadores,
                     PedidoRepository.ClienteLiderRelatorio lider) {
            this.pagina = pagina; this.indicadores = indicadores; this.lider = lider;
        }

        private RelatorioClienteService service() {
            PedidoRepository repository = (PedidoRepository) Proxy.newProxyInstance(
                    getClass().getClassLoader(), new Class[]{PedidoRepository.class}, this
            );
            return new RelatorioClienteService(repository);
        }

        @Override
        public Object invoke(Object proxy, Method metodo, Object[] args) {
            return switch (metodo.getName()) {
                case "buscarRelatorioClientes" -> { argumentosPagina.set(args); yield pagina; }
                case "buscarIndicadoresRelatorioClientes" -> indicadores;
                case "buscarClienteLiderRelatorio" -> lider == null ? List.of() : List.of(lider);
                case "buscarLoteRelatorioClientes" -> {
                    List<RelatorioClienteLinhaResponse> conteudo = loteCheio
                            ? Collections.nCopies(500, linha(1, "Cliente", 1, "1", INICIO, FIM, 1, 0, "1", 1))
                            : List.of();
                    yield new SliceImpl<>(conteudo, (Pageable) args[10], loteCheio);
                }
                default -> null;
            };
        }
    }
}
