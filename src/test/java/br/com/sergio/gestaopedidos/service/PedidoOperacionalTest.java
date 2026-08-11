package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.pedido.ItemPedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoRequest;
import br.com.sergio.gestaopedidos.entity.Cliente;
import br.com.sergio.gestaopedidos.entity.Pedido;
import br.com.sergio.gestaopedidos.entity.Produto;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.mapper.PedidoMapper;
import br.com.sergio.gestaopedidos.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidoOperacionalTest {
    private PedidoRepository pedidos;
    private ClienteRepository clientes;
    private ProdutoRepository produtos;
    private PedidoService service;

    @BeforeEach
    void preparar() {
        pedidos = mock(PedidoRepository.class);
        clientes = mock(ClienteRepository.class);
        produtos = mock(ProdutoRepository.class);
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente()));
        when(clientes.findById(3L)).thenReturn(Optional.of(Cliente.builder().id(3L).nome("Outro cliente")
                .telefone("41988888888").endereco("Rua Nova").numero("99").bairro("Centro")
                .cidade("Curitiba").cep("80000-001").build()));
        when(produtos.findById(2L)).thenReturn(Optional.of(Produto.builder().id(2L).nome("Feijoada")
                .preco(BigDecimal.TEN).ativo(true).vendavel(true).tipoProduto(TipoProduto.PRODUTO_REVENDA)
                .unidadeVenda(UnidadeVenda.QUILOGRAMA).permiteAcompanhamento(false).build()));
        when(pedidos.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        service = new PedidoService(pedidos, clientes, produtos, mock(PedidoMapper.class),
                mock(EstoqueService.class), mock(MovimentacaoEstoqueRepository.class));
    }

    @Test
    void entregaSalvaJanelaESnapshot() {
        service.salvar(request(TipoEntrega.ENTREGA, LocalTime.of(12, 30), LocalTime.of(13, 30)));
        Pedido salvo = capturarSalvo();
        assertThat(salvo.getHorarioInicio()).isEqualTo("12:30");
        assertThat(salvo.getHorarioFim()).isEqualTo("13:30");
        assertThat(salvo.getEnderecoEntregaHistorico()).isEqualTo("Rua X");
        assertThat(salvo.getBairroEntregaHistorico()).isEqualTo("Água Verde");
        assertThat(salvo.getItens().getFirst().getNomeHistorico()).isEqualTo("Feijoada");
        assertThat(salvo.getItens().getFirst().getUnidadeHistorica()).isEqualTo(UnidadeVenda.QUILOGRAMA);
    }

    @Test
    void retiradaSalvaHorarioSemEndereco() {
        service.salvar(request(TipoEntrega.RETIRADA, LocalTime.of(13, 0), null));
        Pedido salvo = capturarSalvo();
        assertThat(salvo.getHorarioInicio()).isEqualTo("13:00");
        assertThat(salvo.getHorarioFim()).isNull();
        assertThat(salvo.getEnderecoEntregaHistorico()).isNull();
    }

    @Test
    void rejeitaFimSemInicioEJanelaInvertida() {
        assertThatThrownBy(() -> service.salvar(request(TipoEntrega.ENTREGA, null, LocalTime.NOON)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("inicial");
        assertThatThrownBy(() -> service.salvar(request(TipoEntrega.ENTREGA, LocalTime.of(14, 0), LocalTime.of(13, 0))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("anterior");
    }

    @Test
    void fluxosDiferenciamEntregaERetiradaEBackendRejeitaSaidaDaRetirada() {
        assertThat(service.transicoesPermitidas(StatusPedido.PRONTO, TipoEntrega.ENTREGA))
                .contains(StatusPedido.SAIU_PARA_ENTREGA).doesNotContain(StatusPedido.ENTREGUE);
        assertThat(service.transicoesPermitidas(StatusPedido.PRONTO, TipoEntrega.RETIRADA))
                .contains(StatusPedido.ENTREGUE).doesNotContain(StatusPedido.SAIU_PARA_ENTREGA);
        Pedido retirada = Pedido.builder().id(9L).status(StatusPedido.PRONTO).tipoEntrega(TipoEntrega.RETIRADA)
                .cliente(cliente()).itens(new ArrayList<>()).build();
        when(pedidos.bloquearDetalhado(9L)).thenReturn(Optional.of(retirada));
        assertThatThrownBy(() -> service.alterarStatus(9L, StatusPedido.SAIU_PARA_ENTREGA, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void entregaProntaPlanejadaSaiNormalmente() {
        Pedido entrega = entregaPronta();
        entrega.confirmarPlanejamento(LocalDateTime.now(), 1);
        when(pedidos.bloquearDetalhado(9L)).thenReturn(Optional.of(entrega));

        service.alterarStatus(9L, StatusPedido.SAIU_PARA_ENTREGA, null, false);

        assertThat(entrega.getStatus()).isEqualTo(StatusPedido.SAIU_PARA_ENTREGA);
        assertThat(entrega.getSaidaSemPlanejamentoEm()).isNull();
    }

    @Test
    void entregaProntaNaoPlanejadaExigeExcecaoExplicita() {
        Pedido entrega = entregaPronta();
        when(pedidos.bloquearDetalhado(9L)).thenReturn(Optional.of(entrega));

        assertThatThrownBy(() -> service.alterarStatus(9L, StatusPedido.SAIU_PARA_ENTREGA, null, false))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Planejamento de Entregas");
        assertThat(entrega.getStatus()).isEqualTo(StatusPedido.PRONTO);

        service.alterarStatus(9L, StatusPedido.SAIU_PARA_ENTREGA, null, true);
        assertThat(entrega.getStatus()).isEqualTo(StatusPedido.SAIU_PARA_ENTREGA);
        assertThat(entrega.getSaidaSemPlanejamentoEm()).isNotNull();
    }

    @Test
    void excecaoNaoPulaOutraValidacaoDeStatusNemAfetaRetirada() {
        Pedido retirada = Pedido.builder().id(9L).status(StatusPedido.PRONTO).tipoEntrega(TipoEntrega.RETIRADA)
                .cliente(cliente()).itens(new ArrayList<>()).build();
        when(pedidos.bloquearDetalhado(9L)).thenReturn(Optional.of(retirada));
        assertThatThrownBy(() -> service.alterarStatus(9L, StatusPedido.ENTREGUE, null, true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("só pode");

        Pedido entregue = entregaPronta();
        entregue.setStatus(StatusPedido.ENTREGUE);
        when(pedidos.bloquearDetalhado(9L)).thenReturn(Optional.of(entregue));
        assertThatThrownBy(() -> service.alterarStatus(9L, StatusPedido.SAIU_PARA_ENTREGA, null, true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Não é permitido");
        assertThat(entregue.getStatus()).isEqualTo(StatusPedido.ENTREGUE);
    }

    @Test
    void snapshotNaoMudaQuandoClienteMuda() {
        service.salvar(request(TipoEntrega.ENTREGA, null, null));
        Pedido salvo = capturarSalvo();
        salvo.getCliente().setEndereco("Rua Nova");
        assertThat(salvo.getEnderecoEntregaHistorico()).isEqualTo("Rua X");
    }

    @Test
    void alteracaoDeItensPesoPagamentoEObservacaoPreservaPlanejamento() {
        Pedido pedido = pedidoEditavelPlanejado();
        when(pedidos.bloquearDetalhado(9L)).thenReturn(Optional.of(pedido));

        PedidoRequest alteracao = PedidoRequest.builder().clienteId(1L).dataAgendada(pedido.getDataAgendada())
                .formaPagamento(FormaPagamento.DINHEIRO).tipoEntrega(TipoEntrega.ENTREGA)
                .horarioInicio(pedido.getHorarioInicio()).horarioFim(pedido.getHorarioFim())
                .taxaEntrega(BigDecimal.ONE).observacao("Observação alterada")
                .itens(List.of(ItemPedidoRequest.builder().produtoId(2L).quantidade(new BigDecimal("1.290")).build()))
                .build();

        service.atualizar(9L, alteracao, Arrays.asList((Long) null));

        assertThat(pedido.isPlanejamentoConfirmado()).isTrue();
        assertThat(pedido.getOrdemPlanejada()).isEqualTo(1);
    }

    @Test
    void alteracaoDeHorarioInvalidaPlanejamento() {
        Pedido pedido = pedidoEditavelPlanejado();
        when(pedidos.bloquearDetalhado(9L)).thenReturn(Optional.of(pedido));
        PedidoRequest alteracao = PedidoRequest.builder().clienteId(1L).dataAgendada(pedido.getDataAgendada())
                .formaPagamento(FormaPagamento.PIX).tipoEntrega(TipoEntrega.ENTREGA)
                .horarioInicio(LocalTime.of(14, 0)).taxaEntrega(BigDecimal.ZERO)
                .itens(List.of(ItemPedidoRequest.builder().produtoId(2L).quantidade(BigDecimal.ONE).build()))
                .build();

        service.atualizar(9L, alteracao, Arrays.asList((Long) null));

        assertThat(pedido.isPlanejamentoConfirmado()).isFalse();
    }

    @Test
    void alteracaoDeEnderecoInvalidaPlanejamento() {
        Pedido pedido = pedidoEditavelPlanejado();
        when(pedidos.bloquearDetalhado(9L)).thenReturn(Optional.of(pedido));
        PedidoRequest alteracao = PedidoRequest.builder().clienteId(3L).dataAgendada(pedido.getDataAgendada())
                .formaPagamento(FormaPagamento.PIX).tipoEntrega(TipoEntrega.ENTREGA)
                .horarioInicio(pedido.getHorarioInicio()).taxaEntrega(BigDecimal.ZERO)
                .itens(List.of(ItemPedidoRequest.builder().produtoId(2L).quantidade(BigDecimal.ONE).build()))
                .build();

        service.atualizar(9L, alteracao, Arrays.asList((Long) null));

        assertThat(pedido.isPlanejamentoConfirmado()).isFalse();
        assertThat(pedido.getEnderecoEntregaHistorico()).isEqualTo("Rua Nova");
    }

    private Pedido capturarSalvo() {
        var captor = org.mockito.ArgumentCaptor.forClass(Pedido.class);
        verify(pedidos, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private PedidoRequest request(TipoEntrega tipo, LocalTime inicio, LocalTime fim) {
        return PedidoRequest.builder().clienteId(1L).dataAgendada(LocalDate.now()).formaPagamento(FormaPagamento.PIX)
                .tipoEntrega(tipo).horarioInicio(inicio).horarioFim(fim).taxaEntrega(BigDecimal.ZERO)
                .itens(List.of(ItemPedidoRequest.builder().produtoId(2L).quantidade(BigDecimal.ONE).build())).build();
    }

    private Cliente cliente() {
        return Cliente.builder().id(1L).nome("Murilo").telefone("41999999999").endereco("Rua X")
                .numero("123").bairro("Água Verde").cidade("Curitiba").cep("80000-000").build();
    }

    private Pedido entregaPronta() {
        return Pedido.builder().id(9L).status(StatusPedido.PRONTO).tipoEntrega(TipoEntrega.ENTREGA)
                .cliente(cliente()).itens(new ArrayList<>()).build();
    }

    private Pedido pedidoEditavelPlanejado() {
        Pedido pedido = Pedido.builder().id(9L).status(StatusPedido.EM_PREPARACAO).tipoEntrega(TipoEntrega.ENTREGA)
                .dataAgendada(LocalDate.now()).formaPagamento(FormaPagamento.PIX)
                .horarioInicio(LocalTime.NOON).subtotal(BigDecimal.ZERO).taxaEntrega(BigDecimal.ZERO)
                .valorTotal(BigDecimal.ZERO).cliente(cliente()).itens(new ArrayList<>()).build();
        pedido.fotografarEnderecoEntrega(pedido.getCliente());
        pedido.confirmarPlanejamento(LocalDateTime.now(), 1);
        return pedido;
    }
}
