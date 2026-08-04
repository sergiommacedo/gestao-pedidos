package br.com.sergio.gestaopedidos.web;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FichaTecnicaTemplateTest {

    private static final Path DIRETORIO = Path.of("src/main/resources/templates/fichas-tecnicas");

    @Test
    void listagemExibeRendimentoECustosDaReceita() throws IOException {
        String html = ler("listar.html");
        assertThat(html).contains("Rendimento esperado", "Custo estimado da receita",
                "Custo estimado por unidade", "Situação do custo");
    }

    @Test
    void detalhesExibemReceitaCompletaEAvisoSobreCustoReal() throws IOException {
        String html = ler("detalhes.html");
        assertThat(html).contains("Rendimento esperado", "Itens totais da receita",
                "Custo estimado da receita", "O custo real por kg será calculado");
    }

    @Test
    void formularioRecebeRendimentoEsperadoSemReceberUnidade() throws IOException {
        String html = ler("formulario.html");
        assertThat(html).contains("Rendimento esperado da receita *", "*{rendimentoEsperado}",
                "Informe quanto esta receita normalmente rende ao final da produção.");
        assertThat(html).doesNotContain("name=\"unidadeRendimento\"");
    }

    @Test
    void templatesNaoMantemConceitoDeBaseDeUmKg() throws IOException {
        String todos = ler("listar.html") + ler("detalhes.html") + ler("formulario.html");
        assertThat(todos).doesNotContain("1 kg do produto", "Base da ficha", "base de 1 kg");
    }

    private String ler(String arquivo) throws IOException {
        return Files.readString(DIRETORIO.resolve(arquivo));
    }
}
