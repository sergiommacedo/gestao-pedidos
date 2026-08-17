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
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class PedidoDescontoMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int itens = jdbcTemplate.update("""
                UPDATE itens_pedido
                   SET preco_unitario_original = preco_unitario,
                       percentual_desconto = 0
                 WHERE preco_unitario_original IS NULL OR percentual_desconto IS NULL
                """);
        int pedidos = jdbcTemplate.update("""
                UPDATE pedidos
                   SET percentual_desconto_geral = 0,
                       valor_desconto_geral = 0
                 WHERE percentual_desconto_geral IS NULL OR valor_desconto_geral IS NULL
                """);
        if (itens > 0 || pedidos > 0) {
            log.info("Compatibilidade de descontos aplicada a {} item(ns) e {} pedido(s) históricos.", itens, pedidos);
        }
    }
}
