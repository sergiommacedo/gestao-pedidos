package br.com.sergio.gestaopedidos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class MovimentacaoEstoqueConstraintMigration implements ApplicationRunner {
    private static final String INDICE = "uk_movimento_pedido_produto_tipo";
    private static final String INDICE_PEDIDO = "idx_movimentacoes_estoque_pedido";
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Set<String> nomes = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet indices = connection.getMetaData().getIndexInfo(connection.getCatalog(), null,
                     "movimentacoes_estoque", false, false)) {
            while (indices.next()) {
                String nome = indices.getString("INDEX_NAME");
                if (nome != null) nomes.add(nome.toLowerCase());
            }
        }

        if (!nomes.contains(INDICE.toLowerCase())) return;

        if (!nomes.contains(INDICE_PEDIDO.toLowerCase())) {
            jdbcTemplate.execute("CREATE INDEX " + INDICE_PEDIDO
                    + " ON movimentacoes_estoque (pedido_id)");
        }

        jdbcTemplate.execute("ALTER TABLE movimentacoes_estoque DROP INDEX " + INDICE);
        log.info("Restrição legada de movimentação por pedido removida.");
    }
}
