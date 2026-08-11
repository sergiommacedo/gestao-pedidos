package br.com.sergio.gestaopedidos.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoKanbanResponsivoTest {
    private static final Path KANBAN = Path.of("src/main/resources/templates/pedidos/kanban.html");
    private static final Path CSS = Path.of("src/main/resources/static/css/app.css");
    private static final Path JS = Path.of("src/main/resources/static/js/app.js");

    @Test
    void mantemCincoColunasInclusiveVazias() throws Exception {
        String html = Files.readString(KANBAN);
        assertThat(html).contains("th:each=\"statusColuna : ${statusKanban}\"", "kanban-vazio",
                "Nenhum pedido", "SAIU_PARA_ENTREGA");
    }

    @Test
    void overflowPertenceAoViewportDoKanbanEColunasTemTrezentosPixels() throws Exception {
        String css = Files.readString(CSS);
        assertThat(css).contains(".kanban-viewport", "overflow-x: auto", "overflow-y: hidden",
                "grid-auto-columns: minmax(300px, 1fr)", "min-width: 300px", "scroll-snap-type: x proximity");
        assertThat(css).contains("body {", "overflow-x: hidden");
        assertThat(css).doesNotContain("body {\n    overflow-x: auto");
    }

    @Test
    void controlesMovemAproximadamenteUmaColunaComScrollSuave() throws Exception {
        String html = Files.readString(KANBAN);
        String js = Files.readString(JS);
        assertThat(html).contains("data-kanban-anterior", "data-kanban-proximo", "data-kanban-viewport", "←", "→");
        assertThat(js).contains("inicializarNavegacaoKanban", "getBoundingClientRect().width",
                "viewport.scrollBy", "behavior: \"smooth\"");
    }

    @Test
    void cardsPreservamConteudoLegivelEmNotebookEDesktop() throws Exception {
        String css = Files.readString(CSS);
        String html = Files.readString(KANBAN);
        assertThat(css).contains("max-width: min(360px", "overflow-wrap: anywhere", "min-width: 100%");
        assertThat(html).contains("pedido.horarioInicio", "pedido.enderecoEntregaResumido",
                "pedido.bairroEntrega", "kanban-itens", "kanban-acoes-secundarias");
    }
}
