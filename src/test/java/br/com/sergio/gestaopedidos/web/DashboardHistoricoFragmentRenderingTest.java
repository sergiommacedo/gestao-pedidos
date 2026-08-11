package br.com.sergio.gestaopedidos.web;

import br.com.sergio.gestaopedidos.dto.dashboard.HistoricoClientePedidosResponse;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.service.DashboardAnaliticoService;
import br.com.sergio.gestaopedidos.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:historico-fragmento;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc(addFilters = false)
class DashboardHistoricoFragmentRenderingTest {

    @Autowired MockMvc mvc;
    @MockitoBean DashboardAnaliticoService dashboardAnaliticoService;
    @MockitoBean DashboardService dashboardService;

    @Test
    void processaFragmentoRealComEntregaRetiradaValoresEPaginacao() throws Exception {
        var pedidos = List.of(
                pedido(31L, TipoEntrega.ENTREGA, LocalTime.of(12, 30)),
                pedido(30L, TipoEntrega.RETIRADA, null));
        var historico = new HistoricoClientePedidosResponse(7L, "Hugo Souza", 2,
                new BigDecimal("121.50"), new BigDecimal("60.75"), pedidos, 0, 2,
                HistoricoClientePedidosResponse.Periodo.ULTIMOS_7_DIAS,
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 11));
        when(dashboardAnaliticoService.buscarHistoricoCliente(7L, 0, 5,
                HistoricoClientePedidosResponse.Periodo.ULTIMOS_7_DIAS, LocalDate.of(2026, 8, 11))).thenReturn(historico);

        mvc.perform(get("/dashboard/clientes/7/pedidos").param("pagina", "0").param("tamanho", "5")
                        .param("dataReferencia", "2026-08-11"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Pedidos de Hugo Souza"),
                        org.hamcrest.Matchers.containsString("Entrega"),
                        org.hamcrest.Matchers.containsString("Retirada"),
                        org.hamcrest.Matchers.containsString("Período: 05/08/2026 a 11/08/2026"),
                        org.hamcrest.Matchers.containsString("Todo o histórico"),
                        org.hamcrest.Matchers.containsString("12:30"),
                        org.hamcrest.Matchers.containsString("Página 1 de 2"),
                        org.hamcrest.Matchers.containsString("pagina=1"))));
    }

    @Test
    void processaEstadoVazioDoFragmentoReal() throws Exception {
        var historico = new HistoricoClientePedidosResponse(8L, "Sem Pedidos", 0,
                BigDecimal.ZERO, BigDecimal.ZERO, List.of(), 0, 0,
                HistoricoClientePedidosResponse.Periodo.ULTIMOS_7_DIAS,
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 11));
        when(dashboardAnaliticoService.buscarHistoricoCliente(8L, 0, 5,
                HistoricoClientePedidosResponse.Periodo.ULTIMOS_7_DIAS, LocalDate.of(2026, 8, 11))).thenReturn(historico);

        mvc.perform(get("/dashboard/clientes/8/pedidos").param("dataReferencia", "2026-08-11"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhum pedido válido encontrado.")));
    }

    @Test
    void processaFragmentoRealDosItensHistoricos() throws Exception {
        var detalhes = new HistoricoClientePedidosResponse.DetalhesItens(List.of(
                new HistoricoClientePedidosResponse.Item("Joelho de Porco", new BigDecimal("1.300"),
                        UnidadeVenda.QUILOGRAMA, new BigDecimal("55.00"), new BigDecimal("71.50")),
                new HistoricoClientePedidosResponse.Item("Frango Assado", BigDecimal.ONE,
                        UnidadeVenda.UNIDADE, new BigDecimal("50.00"), new BigDecimal("50.00"))),
                new BigDecimal("121.50"), new BigDecimal("10.00"), new BigDecimal("131.50"));
        when(dashboardAnaliticoService.buscarItensHistoricos(7L, 31L)).thenReturn(detalhes);

        mvc.perform(get("/dashboard/clientes/7/pedidos/31/itens"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Joelho de Porco"),
                        org.hamcrest.Matchers.containsString("1,300 kg"),
                        org.hamcrest.Matchers.containsString("R$ 55,00/kg"),
                        org.hamcrest.Matchers.containsString("Frango Assado"),
                        org.hamcrest.Matchers.containsString("R$ 50,00/un"),
                        org.hamcrest.Matchers.containsString("R$ 71,50"),
                        org.hamcrest.Matchers.containsString("R$ 10,00"),
                        org.hamcrest.Matchers.containsString("R$ 131,50"))));
    }

    private HistoricoClientePedidosResponse.Pedido pedido(Long id, TipoEntrega tipo, LocalTime horario) {
        return new HistoricoClientePedidosResponse.Pedido(id, LocalDate.of(2026, 8, 11), horario, tipo,
                StatusPedido.ENTREGUE, new BigDecimal("55.75"), new BigDecimal("5.00"),
                new BigDecimal("60.75"));
    }
}
