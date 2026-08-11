package br.com.sergio.gestaopedidos.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardAnaliticoRankingTest {

    @Test
    void calculaQuantidadeValorETicketNoPeriodoSemCancelados() {
        var banco = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(banco);
            jdbc.execute("CREATE TABLE clientes (id BIGINT PRIMARY KEY, nome VARCHAR(100))");
            jdbc.execute("CREATE TABLE pedidos (id BIGINT PRIMARY KEY, cliente_id BIGINT, data_agendada DATE, status VARCHAR(30), valor_total DECIMAL(10,2))");
            jdbc.update("INSERT INTO clientes VALUES (1,'Hugo Souza'),(2,'Ana Lima')");
            jdbc.update("INSERT INTO pedidos VALUES (1,1,'2026-08-05','ENTREGUE',500.00),(2,1,'2026-08-11','PRONTO',75.00),(3,1,'2026-08-11','CANCELADO',900.00),(4,2,'2026-08-10','PENDENTE',100.00),(5,2,'2026-08-01','ENTREGUE',1000.00)");

            var ranking = new DashboardAnaliticoService(jdbc).buscarRankingClientes(
                    java.time.LocalDate.of(2026, 8, 5), java.time.LocalDate.of(2026, 8, 11));

            assertThat(ranking).hasSize(2);
            assertThat(ranking.getFirst().nome()).isEqualTo("Hugo Souza");
            assertThat(ranking.getFirst().quantidadePedidos()).isEqualTo(2);
            assertThat(ranking.getFirst().valorTotal()).isEqualByComparingTo("575.00");
            assertThat(ranking.getFirst().ticketMedio()).isEqualByComparingTo("287.50");
            assertThat(ranking.get(1).valorTotal()).isEqualByComparingTo("100.00");
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
                "ORDER BY valor_total DESC LIMIT 5",
                "LocalDate inicio = data.minusDays(6)");
        assertThat(fonte).doesNotContain("pedidoRepository.findAll", "FROM Pedido p");
    }
}
