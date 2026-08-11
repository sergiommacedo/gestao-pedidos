package br.com.sergio.gestaopedidos.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PlanejamentoEntregaTemplateTest {
    private static final Path PLANEJAMENTO = Path.of("src/main/resources/templates/pedidos/planejamento.html");
    private static final Path LISTA = Path.of("src/main/resources/templates/pedidos/listar.html");
    private static final Path KANBAN = Path.of("src/main/resources/templates/pedidos/kanban.html");
    private static final Path FORMULARIO = Path.of("src/main/resources/templates/pedidos/formulario.html");
    private static final Path JS = Path.of("src/main/resources/static/js/app.js");

    @Test
    void formularioUsaHorariosNativosEEdicaoTemBinding() throws Exception {
        assertThat(Files.readString(FORMULARIO)).contains("type=\"time\"", "th:field=\"*{horarioInicio}\"", "th:field=\"*{horarioFim}\"");
    }

    @Test
    void listaEKanbanSoMostramEnderecoNoBlocoEntrega() throws Exception {
        assertThat(Files.readString(LISTA)).contains("pedido.tipoEntrega.name() == 'ENTREGA'", "pedido.enderecoEntregaResumido", "Planejar entregas");
        assertThat(Files.readString(KANBAN)).contains("pedido.tipoEntrega.name() == 'ENTREGA'", "pedido.enderecoEntregaResumido", "Concluir retirada", "Saiu para entrega");
    }

    @Test
    void planejamentoDestacaBairroInvalidosEOrdemPreliminar() throws Exception {
        assertThat(Files.readString(PLANEJAMENTO)).contains("Ordem preliminar por horário", "text-uppercase text-primary",
                "Endereço incompleto", "data-mover-cima", "data-mover-baixo", "Já em rota");
    }

    @Test
    void javascriptReordenaNosDoisSentidosERespeitaOrdemNaUrl() throws Exception {
        assertThat(Files.readString(JS)).contains("cartao.previousElementSibling", "cartao.nextElementSibling",
                "enderecos.slice(0, -1).join(\"|\")", "Math.min(4", "url.length <= 2048",
                "https://www.google.com/maps/dir/?");
    }

    @Test
    void nenhumaApiPagaOuChaveFoiIntegrada() throws Exception {
        String javaScript = Files.readString(JS);
        String planejamento = Files.readString(PLANEJAMENTO);
        assertThat(javaScript + planejamento).doesNotContain("Route Optimization", "routes.googleapis.com",
                "maps.googleapis.com", "DirectionsService", "apiKey", "parametros.set(\"key\"");
    }
}
