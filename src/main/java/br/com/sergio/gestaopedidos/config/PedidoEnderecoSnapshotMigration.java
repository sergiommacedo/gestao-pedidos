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
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class PedidoEnderecoSnapshotMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int atualizados = jdbcTemplate.update("""
                UPDATE pedidos p
                SET endereco_entrega_historico = (SELECT c.endereco FROM clientes c WHERE c.id = p.cliente_id),
                    numero_entrega_historico = (SELECT c.numero FROM clientes c WHERE c.id = p.cliente_id),
                    bairro_entrega_historico = (SELECT c.bairro FROM clientes c WHERE c.id = p.cliente_id),
                    cidade_entrega_historico = (SELECT c.cidade FROM clientes c WHERE c.id = p.cliente_id),
                    cep_entrega_historico = (SELECT c.cep FROM clientes c WHERE c.id = p.cliente_id),
                    complemento_entrega_historico = (SELECT c.complemento FROM clientes c WHERE c.id = p.cliente_id)
                WHERE p.tipo_entrega = 'ENTREGA'
                  AND p.endereco_entrega_historico IS NULL
                  AND p.numero_entrega_historico IS NULL
                  AND p.bairro_entrega_historico IS NULL
                  AND p.cidade_entrega_historico IS NULL
                """);
        if (atualizados > 0) {
            log.info("Snapshot inicial de endereço criado para {} pedido(s) de entrega.", atualizados);
        }
    }
}
