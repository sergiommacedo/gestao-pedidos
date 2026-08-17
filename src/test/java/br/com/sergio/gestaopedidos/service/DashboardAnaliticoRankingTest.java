package br.com.sergio.gestaopedidos.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardAnaliticoRankingTest {

    @Test
    void calculaQuantidadeValorETicketEmTodoHistoricoSemCancelados() {
        var banco = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(banco);
            jdbc.execute("CREATE TABLE clientes (id BIGINT PRIMARY KEY, nome VARCHAR(100))");
            jdbc.execute("CREATE TABLE pedidos (id BIGINT PRIMARY KEY, cliente_id BIGINT, data_agendada DATE, status VARCHAR(30), valor_total DECIMAL(10,2))");
            jdbc.update("INSERT INTO clientes VALUES (1,'Hugo Souza'),(2,'Ana Lima')");
            jdbc.update("INSERT INTO pedidos VALUES (1,1,'2026-08-05','ENTREGUE',500.00),(2,1,'2026-08-11','PRONTO',75.00),(3,1,'2026-08-11','CANCELADO',900.00),(4,2,'2026-08-10','PENDENTE',100.00),(5,2,'2026-08-01','ENTREGUE',1000.00)");

            var ranking = new DashboardAnaliticoService(jdbc).buscarRankingClientes();

            assertThat(ranking).hasSize(2);
            assertThat(ranking.getFirst().nome()).isEqualTo("Ana Lima");
            assertThat(ranking.getFirst().quantidadePedidos()).isEqualTo(2);
            assertThat(ranking.getFirst().valorTotal()).isEqualByComparingTo("1100.00");
            assertThat(ranking.getFirst().ticketMedio()).isEqualByComparingTo("550.00");
            assertThat(ranking.get(1).nome()).isEqualTo("Hugo Souza");
            assertThat(ranking.get(1).valorTotal()).isEqualByComparingTo("575.00");
        } finally {
            banco.shutdown();
        }
    }

    @Test
    void rankingAgregaNoBancoExcluiCanceladosOrdenaPorValorELimitaTopCinco() throws Exception {
        String fonte = Files.readString(Path.of(
                "src/main/java/br/com/sergio/gestaopedidos/service/DashboardAnaliticoService.java"));

        assertThat(fonte).contains("COUNT(*) quantidade_pedidos",
                "SUM(p.valor_total)",
                "SUM(p.valor_total)/NULLIF(COUNT(*),0)",
                "p.status<>'CANCELADO'",
                "GROUP BY c.id,c.nome",
                "ORDER BY valor_total DESC,quantidade_pedidos DESC,c.nome ASC LIMIT 5",
                "LocalDate inicio = data.minusDays(6)");
        assertThat(fonte).doesNotContain("pedidoRepository.findAll", "FROM Pedido p");
    }

    @Test
    void historicoPaginaNoBancoIsolaClienteExcluiCanceladosECarregaItensSobDemanda() {
        var banco = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(banco);
            jdbc.execute("CREATE TABLE clientes (id BIGINT PRIMARY KEY,nome VARCHAR(100))");
            jdbc.execute("CREATE TABLE produtos (id BIGINT PRIMARY KEY,nome VARCHAR(100),unidade_venda VARCHAR(20))");
            jdbc.execute("CREATE TABLE pedidos (id BIGINT PRIMARY KEY,cliente_id BIGINT,data_agendada DATE,data_pedido TIMESTAMP,horario_inicio TIME,tipo_entrega VARCHAR(20),status VARCHAR(30),subtotal DECIMAL(10,2),percentual_desconto_geral DECIMAL(7,4),valor_desconto_geral DECIMAL(10,2),taxa_entrega DECIMAL(10,2),valor_total DECIMAL(10,2))");
            jdbc.execute("CREATE TABLE itens_pedido (id BIGINT PRIMARY KEY,pedido_id BIGINT,produto_id BIGINT,nome_historico VARCHAR(100),unidade_historica VARCHAR(20),quantidade DECIMAL(10,3),preco_unitario_original DECIMAL(10,2),percentual_desconto DECIMAL(7,4),preco_unitario DECIMAL(10,2),subtotal DECIMAL(10,2))");
            jdbc.update("INSERT INTO clientes VALUES (1,'Hugo Souza'),(2,'Outro Cliente')");
            jdbc.update("INSERT INTO produtos VALUES (10,'Nome atual','UNIDADE'),(11,'Joelho atual','QUILOGRAMA')");
            jdbc.update("INSERT INTO pedidos VALUES (20,1,'2026-08-10','2026-08-10 12:00:00','12:00','ENTREGA','ENTREGUE',121.50,0,0,10.00,131.50),(21,1,'2026-08-11','2026-08-11 14:00:00','14:00','RETIRADA','PRONTO',50.00,0,0,0.00,50.00),(22,1,'2026-08-12','2026-08-12 10:00:00','10:00','ENTREGA','CANCELADO',999.00,0,0,1.00,1000.00),(30,2,'2026-08-13','2026-08-13 10:00:00','10:00','ENTREGA','ENTREGUE',700.00,0,0,0.00,700.00)");
            jdbc.update("INSERT INTO itens_pedido VALUES (100,20,10,'Frango Assado','UNIDADE',1.000,50.00,0,50.00,50.00),(101,20,11,'Joelho de Porco','QUILOGRAMA',1.300,55.00,0,55.00,71.50)");
            DashboardAnaliticoService service = new DashboardAnaliticoService(jdbc);

            var primeira = service.buscarHistoricoCliente(1L, 0, 1,
                    br.com.sergio.gestaopedidos.dto.dashboard.HistoricoClientePedidosResponse.Periodo.TODO_HISTORICO,
                    java.time.LocalDate.of(2026, 8, 11));
            var segunda = service.buscarHistoricoCliente(1L, 1, 1,
                    br.com.sergio.gestaopedidos.dto.dashboard.HistoricoClientePedidosResponse.Periodo.TODO_HISTORICO,
                    java.time.LocalDate.of(2026, 8, 11));

            assertThat(primeira.clienteNome()).isEqualTo("Hugo Souza");
            assertThat(primeira.quantidadeTotal()).isEqualTo(2);
            assertThat(primeira.valorTotal()).isEqualByComparingTo("181.50");
            assertThat(primeira.ticketMedio()).isEqualByComparingTo("90.75");
            assertThat(primeira.totalPaginas()).isEqualTo(2);
            assertThat(primeira.pedidos()).singleElement().satisfies(pedido -> {
                assertThat(pedido.id()).isEqualTo(21L);
                assertThat(pedido.data()).isEqualTo(java.time.LocalDate.of(2026, 8, 11));
                assertThat(pedido.tipoEntrega().name()).isEqualTo("RETIRADA");
                assertThat(pedido.valorTotal()).isEqualByComparingTo("50.00");
            });
            assertThat(segunda.pedidos()).extracting(p -> p.id()).containsExactly(20L);

            var detalhes = service.buscarItensHistoricos(1L, 20L);
            assertThat(detalhes.itens()).hasSize(2);
            assertThat(detalhes.itens().get(0).produtoNome()).isEqualTo("Frango Assado");
            assertThat(detalhes.itens().get(0).unidade().name()).isEqualTo("UNIDADE");
            assertThat(detalhes.itens().get(0).precoUnitario()).isEqualByComparingTo("50.00");
            assertThat(detalhes.itens().get(1).produtoNome()).isEqualTo("Joelho de Porco");
            assertThat(detalhes.itens().get(1).quantidade()).isEqualByComparingTo("1.300");
            assertThat(detalhes.itens().get(1).subtotal()).isEqualByComparingTo("71.50");
            assertThat(detalhes.taxaEntrega()).isEqualByComparingTo("10.00");
            assertThat(detalhes.valorTotal()).isEqualByComparingTo("131.50");
            assertThatThrownBy(() -> service.buscarItensHistoricos(2L, 20L))
                    .isInstanceOf(br.com.sergio.gestaopedidos.exception.ResourceNotFoundException.class);
        } finally {
            banco.shutdown();
        }
    }

    @Test
    void rankingModalInicialETodoHistoricoUsamConjuntosConsistentesNoCenarioReal() {
        var banco = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).generateUniqueName(true).build();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(banco);
            jdbc.execute("CREATE TABLE clientes (id BIGINT PRIMARY KEY,nome VARCHAR(100))");
            jdbc.execute("CREATE TABLE pedidos (id BIGINT PRIMARY KEY,cliente_id BIGINT,data_agendada DATE,data_pedido TIMESTAMP,horario_inicio TIME,tipo_entrega VARCHAR(20),status VARCHAR(30),subtotal DECIMAL(10,2),percentual_desconto_geral DECIMAL(7,4),valor_desconto_geral DECIMAL(10,2),taxa_entrega DECIMAL(10,2),valor_total DECIMAL(10,2))");
            jdbc.update("INSERT INTO clientes VALUES (1,'Murilo Macedo')");
            jdbc.update("INSERT INTO pedidos VALUES (2,1,'2026-08-04','2026-08-04 10:00:00','10:00','RETIRADA','ENTREGUE',55.00,0,0,0.00,55.00),(8,1,'2026-08-11','2026-08-11 08:00:00','08:00','ENTREGA','ENTREGUE',90.00,0,0,10.00,100.00),(12,1,'2026-08-11','2026-08-11 09:00:00','09:00','RETIRADA','ENTREGUE',83.51,0,0,0.00,83.51),(14,1,'2026-08-11','2026-08-11 10:00:00','10:00','ENTREGA','PRONTO',90.00,0,0,10.00,100.00),(15,1,'2026-08-11','2026-08-11 11:00:00','11:00','ENTREGA','CANCELADO',999.00,0,0,0.00,999.00)");
            var service = new DashboardAnaliticoService(jdbc);
            var referencia = java.time.LocalDate.of(2026, 8, 11);

            var ranking = service.buscarRankingClientes().getFirst();
            var modal = service.buscarHistoricoCliente(1L, 0, 5,
                    br.com.sergio.gestaopedidos.dto.dashboard.HistoricoClientePedidosResponse.Periodo.ULTIMOS_7_DIAS,
                    referencia);
            var completo = service.buscarHistoricoCliente(1L, 0, 5,
                    br.com.sergio.gestaopedidos.dto.dashboard.HistoricoClientePedidosResponse.Periodo.TODO_HISTORICO,
                    referencia);

            assertThat(completo.quantidadeTotal()).isEqualTo(ranking.quantidadePedidos()).isEqualTo(4);
            assertThat(completo.valorTotal()).isEqualByComparingTo(ranking.valorTotal()).isEqualByComparingTo("338.51");
            assertThat(completo.ticketMedio()).isEqualByComparingTo(ranking.ticketMedio());
            assertThat(modal.pedidos()).extracting(p -> p.id()).containsExactly(14L, 12L, 8L).doesNotContain(2L, 15L);
            assertThat(completo.quantidadeTotal()).isEqualTo(4);
            assertThat(completo.valorTotal()).isEqualByComparingTo("338.51");
            assertThat(completo.ticketMedio()).isEqualByComparingTo("84.627500");
            assertThat(completo.pedidos()).extracting(p -> p.id()).contains(2L).doesNotContain(15L);
        } finally {
            banco.shutdown();
        }
    }
}
