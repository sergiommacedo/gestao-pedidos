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
public class ProdutoClassificacaoMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int tipos = jdbcTemplate.update("UPDATE produtos SET tipo_produto = 'PRODUZIDO' WHERE tipo_produto IS NULL");
        int vendaveis = jdbcTemplate.update("UPDATE produtos SET vendavel = true WHERE vendavel IS NULL");
        if (tipos > 0 || vendaveis > 0) {
            log.info("Classificação de produtos antigos concluída: {} tipo(s) e {} disponibilidade(s) preenchidos.", tipos, vendaveis);
        }
    }
}
