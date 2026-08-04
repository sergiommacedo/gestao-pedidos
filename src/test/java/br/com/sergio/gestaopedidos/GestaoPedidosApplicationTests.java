package br.com.sergio.gestaopedidos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import br.com.sergio.gestaopedidos.service.DashboardAnaliticoService;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:gestao-pedidos;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class GestaoPedidosApplicationTests {

    @Autowired
    private DashboardAnaliticoService dashboardAnaliticoService;

    @Test
    void contextLoads() {
    }

    @Test
    void consultasDoDashboardCarregamNoContextoReal() {
        var dashboard = dashboardAnaliticoService.buscar(LocalDate.of(2026, 8, 4));
        assertThat(dashboard.vendasPorDia()).isEmpty();
        assertThat(dashboard.producaoPorDia()).isEmpty();
        assertThat(dashboard.lucroBrutoEstimado()).isZero();
    }

}
