package br.com.sergio.gestaopedidos.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Migração lógica temporária enquanto valor_ingredientes ainda existe no banco. */
@Component
@RequiredArgsConstructor
public class ProducaoMateriaisMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.update("""
                UPDATE producoes
                   SET saldo_inicial_materiais = 0,
                       valor_compras_materiais = valor_ingredientes,
                       saldo_final_materiais = 0
                 WHERE saldo_inicial_materiais IS NULL
                   AND valor_compras_materiais IS NULL
                   AND saldo_final_materiais IS NULL
                """);
        jdbcTemplate.update("UPDATE producoes SET saldo_inicial_materiais = 0 WHERE saldo_inicial_materiais IS NULL");
        jdbcTemplate.update("UPDATE producoes SET valor_compras_materiais = 0 WHERE valor_compras_materiais IS NULL");
        jdbcTemplate.update("UPDATE producoes SET saldo_final_materiais = 0 WHERE saldo_final_materiais IS NULL");
    }
}
