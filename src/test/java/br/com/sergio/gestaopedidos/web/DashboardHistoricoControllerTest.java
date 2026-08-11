package br.com.sergio.gestaopedidos.web;

import br.com.sergio.gestaopedidos.controller.web.DashboardController;
import br.com.sergio.gestaopedidos.dto.dashboard.HistoricoClientePedidosResponse;
import br.com.sergio.gestaopedidos.exception.GlobalExceptionHandler;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import br.com.sergio.gestaopedidos.service.DashboardAnaliticoService;
import br.com.sergio.gestaopedidos.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DashboardHistoricoControllerTest {

    @Test
    void endpointRespondePaginaDoClienteCorretoInclusiveVazia() throws Exception {
        DashboardAnaliticoService service = mock(DashboardAnaliticoService.class);
        var vazio = new HistoricoClientePedidosResponse(7L, "Hugo Souza", 0, BigDecimal.ZERO,
                BigDecimal.ZERO, List.of(), 0, 0);
        when(service.buscarHistoricoCliente(7L, 0, 5)).thenReturn(vazio);
        MockMvc mvc = mvc(service);

        mvc.perform(get("/dashboard/clientes/7/pedidos").param("pagina", "0").param("tamanho", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/fragments/historico-cliente :: conteudo"))
                .andExpect(model().attribute("historico", vazio));

        verify(service).buscarHistoricoCliente(7L, 0, 5);
        verify(service, never()).buscarHistoricoCliente(eq(8L), anyInt(), anyInt());
    }

    @Test
    void endpointRetorna404ParaClienteInexistente() throws Exception {
        DashboardAnaliticoService service = mock(DashboardAnaliticoService.class);
        when(service.buscarHistoricoCliente(999L, 0, 5)).thenThrow(new ResourceNotFoundException("Cliente não encontrado."));

        mvc(service).perform(get("/dashboard/clientes/999/pedidos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void erroGenericoMantemRespostaApiErrorGravavelMesmoQuandoClienteAceitaHtml() throws Exception {
        DashboardAnaliticoService service = mock(DashboardAnaliticoService.class);
        when(service.buscarHistoricoCliente(7L, 0, 5)).thenThrow(new IllegalStateException("Falha simulada"));

        mvc(service).perform(get("/dashboard/clientes/7/pedidos").accept(MediaType.TEXT_HTML))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor."));
    }

    private MockMvc mvc(DashboardAnaliticoService service) {
        return standaloneSetup(new DashboardController(mock(DashboardService.class), service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
}
