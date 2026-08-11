package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.entity.Cliente;
import br.com.sergio.gestaopedidos.entity.Pedido;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.mapper.PedidoMapper;
import br.com.sergio.gestaopedidos.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PedidoPlanejamentoServiceTest {
    private PedidoRepository pedidos;
    private PedidoService service;

    @BeforeEach
    void preparar() {
        pedidos = mock(PedidoRepository.class);
        PedidoMapper mapper = mock(PedidoMapper.class, Answers.CALLS_REAL_METHODS);
        service = new PedidoService(pedidos, mock(ClienteRepository.class), mock(ProdutoRepository.class), mapper,
                mock(EstoqueService.class), mock(MovimentacaoEstoqueRepository.class));
    }

    @Test
    void nenhumaEntregaTemTodosOsContadoresZerados() {
        when(pedidos.buscarParaPlanejamento(any(), anyCollection())).thenReturn(List.of());
        var resultado = service.planejarEntregas(LocalDate.now());
        assertThat(resultado.quantidadeElegiveis()).isZero();
        assertThat(resultado.quantidadeEnderecosNavegaveis()).isZero();
        assertThat(resultado.quantidadeEnderecosIncompletos()).isZero();
        assertThat(resultado.quantidadeEmRota()).isZero();
    }

    @Test
    void somentePedidoEmRotaNaoContaComoElegivel() {
        when(pedidos.buscarParaPlanejamento(any(), anyCollection())).thenReturn(List.of(pedido(1, StatusPedido.SAIU_PARA_ENTREGA, true)));
        var resultado = service.planejarEntregas(LocalDate.now());
        assertThat(resultado.quantidadeElegiveis()).isZero();
        assertThat(resultado.quantidadeEmRota()).isOne();
        assertThat(resultado.emRota()).hasSize(1);
    }

    @Test
    void entregasElegiveisTodasSemEnderecoSaoContadasComoIncompletas() {
        when(pedidos.buscarParaPlanejamento(any(), anyCollection())).thenReturn(List.of(
                pedido(1, StatusPedido.PENDENTE, false), pedido(2, StatusPedido.PRONTO, false)));
        var resultado = service.planejarEntregas(LocalDate.now());
        assertThat(resultado.quantidadeElegiveis()).isEqualTo(2);
        assertThat(resultado.quantidadeEnderecosNavegaveis()).isZero();
        assertThat(resultado.quantidadeEnderecosIncompletos()).isEqualTo(2);
    }

    @Test
    void planejamentoContaEnderecosValidosEInvalidosSemOcultarPedidos() {
        when(pedidos.buscarParaPlanejamento(any(), anyCollection())).thenReturn(List.of(
                pedido(1, StatusPedido.PENDENTE, true), pedido(2, StatusPedido.EM_PREPARACAO, false),
                pedido(3, StatusPedido.PRONTO, true)));
        var resultado = service.planejarEntregas(LocalDate.now());
        assertThat(resultado.elegiveis()).hasSize(3);
        assertThat(resultado.quantidadeEnderecosNavegaveis()).isEqualTo(2);
        assertThat(resultado.quantidadeEnderecosIncompletos()).isOne();
    }

    @Test
    void todasAsEntregasElegiveisComEnderecoSaoNavegaveis() {
        when(pedidos.buscarParaPlanejamento(any(), anyCollection())).thenReturn(List.of(
                pedido(1, StatusPedido.PENDENTE, true), pedido(2, StatusPedido.PRONTO, true)));
        var resultado = service.planejarEntregas(LocalDate.now());
        assertThat(resultado.quantidadeElegiveis()).isEqualTo(2);
        assertThat(resultado.quantidadeEnderecosNavegaveis()).isEqualTo(2);
        assertThat(resultado.quantidadeEnderecosIncompletos()).isZero();
    }

    @Test
    void confirmacaoMarcaSomentePedidosValidosNaOrdemInformada() {
        LocalDate data = LocalDate.now();
        Pedido valido2 = pedido(2, StatusPedido.PRONTO, true);
        Pedido invalido = pedido(3, StatusPedido.PRONTO, false);
        Pedido valido1 = pedido(1, StatusPedido.EM_PREPARACAO, true);
        when(pedidos.buscarParaPlanejamento(eq(data), anyCollection()))
                .thenReturn(List.of(valido1, valido2, invalido));

        service.confirmarPlanejamento(data, List.of(2L, 3L, 1L));

        assertThat(valido2.getOrdemPlanejada()).isEqualTo(1);
        assertThat(valido1.getOrdemPlanejada()).isEqualTo(2);
        assertThat(valido1.getPlanejadoEm()).isNotNull();
        assertThat(invalido.isPlanejamentoConfirmado()).isFalse();
        verify(pedidos).saveAll(anyList());
    }

    @Test
    void replanejamentoDesmarcaPedidoRemovidoEIgnoraPedidoJaEmRota() {
        LocalDate data = LocalDate.now();
        Pedido removido = pedido(1, StatusPedido.PRONTO, true);
        removido.confirmarPlanejamento(LocalDateTime.now().minusHours(1), 1);
        Pedido emRota = pedido(2, StatusPedido.SAIU_PARA_ENTREGA, true);
        emRota.confirmarPlanejamento(LocalDateTime.now().minusHours(1), 2);
        when(pedidos.buscarParaPlanejamento(eq(data), anyCollection())).thenReturn(List.of(removido, emRota));

        service.confirmarPlanejamento(data, List.of(2L, 999L));

        assertThat(removido.isPlanejamentoConfirmado()).isFalse();
        assertThat(emRota.isPlanejamentoConfirmado()).isTrue();
        assertThat(emRota.getOrdemPlanejada()).isEqualTo(2);
    }

    private Pedido pedido(long id, StatusPedido status, boolean enderecoCompleto) {
        Cliente cliente = Cliente.builder().id(id).nome("Cliente " + id).telefone("41999999999").build();
        return Pedido.builder().id(id).cliente(cliente).tipoEntrega(TipoEntrega.ENTREGA).status(status)
                .enderecoEntregaHistorico(enderecoCompleto ? "Rua X" : null)
                .numeroEntregaHistorico(enderecoCompleto ? "123" : null)
                .bairroEntregaHistorico(enderecoCompleto ? "Centro" : null)
                .cidadeEntregaHistorico(enderecoCompleto ? "Curitiba" : null).build();
    }
}
