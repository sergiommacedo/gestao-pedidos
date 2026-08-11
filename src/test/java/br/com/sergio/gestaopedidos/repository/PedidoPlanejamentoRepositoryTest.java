package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.*;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.generate_statistics=true"})
class PedidoPlanejamentoRepositoryTest {
    @Autowired PedidoRepository pedidos;
    @Autowired ClienteRepository clientes;
    @Autowired EntityManager em;
    private Cliente cliente;
    private final LocalDate data = LocalDate.of(2026, 8, 11);

    @BeforeEach
    void preparar() {
        cliente = clientes.save(Cliente.builder().nome("Cliente").telefone("41999999999").endereco("Rua X")
                .numero("123").bairro("Centro").cidade("Curitiba").build());
    }

    @Test
    void planejamentoExcluiRetiradaCanceladoEEntregueEOrdenaSemHorarioDepois() {
        Pedido semHorario = salvar(TipoEntrega.ENTREGA, StatusPedido.PENDENTE, null);
        Pedido tarde = salvar(TipoEntrega.ENTREGA, StatusPedido.PRONTO, LocalTime.of(13, 30));
        Pedido cedo = salvar(TipoEntrega.ENTREGA, StatusPedido.EM_PREPARACAO, LocalTime.of(11, 30));
        Pedido emRota = salvar(TipoEntrega.ENTREGA, StatusPedido.SAIU_PARA_ENTREGA, LocalTime.of(12, 0));
        salvar(TipoEntrega.RETIRADA, StatusPedido.PENDENTE, LocalTime.of(10, 0));
        salvar(TipoEntrega.ENTREGA, StatusPedido.CANCELADO, LocalTime.of(9, 0));
        salvar(TipoEntrega.ENTREGA, StatusPedido.ENTREGUE, LocalTime.of(8, 0));

        var resultado = pedidos.buscarParaPlanejamento(data, EnumSet.of(StatusPedido.PENDENTE,
                StatusPedido.EM_PREPARACAO, StatusPedido.PRONTO, StatusPedido.SAIU_PARA_ENTREGA));

        assertThat(resultado).extracting(Pedido::getId)
                .containsExactly(cedo.getId(), emRota.getId(), tarde.getId(), semHorario.getId());
        assertThat(resultado).allMatch(p -> p.getTipoEntrega() == TipoEntrega.ENTREGA)
                .noneMatch(p -> p.getStatus() == StatusPedido.CANCELADO || p.getStatus() == StatusPedido.ENTREGUE);
        assertThat(resultado.getFirst().getBairroEntregaHistorico()).isEqualTo("Centro");
    }

    @Test
    void listagemCarregaClientesEItensEmQuantidadeConstanteDeConsultas() {
        for (int i = 0; i < 8; i++) salvar(TipoEntrega.ENTREGA, StatusPedido.PENDENTE, LocalTime.NOON);
        em.flush();
        em.clear();
        var estatisticas = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        estatisticas.clear();

        var pagina = pedidos.buscarPaginado("", null, data, null, null, "", PageRequest.of(0, 20));
        pagina.forEach(p -> { p.getCliente().getNome(); p.getItens().size(); });

        assertThat(estatisticas.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    private Pedido salvar(TipoEntrega tipo, StatusPedido status, LocalTime horario) {
        Pedido pedido = Pedido.builder().dataPedido(LocalDateTime.now()).dataAgendada(data).status(status)
                .formaPagamento(FormaPagamento.PIX).tipoEntrega(tipo).horarioInicio(horario)
                .subtotal(BigDecimal.ZERO).taxaEntrega(BigDecimal.ZERO).valorTotal(BigDecimal.ZERO)
                .cliente(cliente).itens(new ArrayList<>()).build();
        pedido.fotografarEnderecoEntrega(cliente);
        return pedidos.save(pedido);
    }
}
