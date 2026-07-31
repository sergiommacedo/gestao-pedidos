package br.com.sergio.gestaopedidos.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class PedidoStatusMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int pedidosMigrados = jdbcTemplate.update("""
                UPDATE pedidos
                SET status = 'PENDENTE'
                WHERE status = 'CONFIRMADO'
                """);

        if (pedidosMigrados > 0) {
            log.info(
                    "Migração de status concluída: {} pedido(s) alterado(s) de CONFIRMADO para PENDENTE.",
                    pedidosMigrados
            );
        }
    }
}
