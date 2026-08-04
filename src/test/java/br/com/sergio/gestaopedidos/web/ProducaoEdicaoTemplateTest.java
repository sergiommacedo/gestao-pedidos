package br.com.sergio.gestaopedidos.web;

import br.com.sergio.gestaopedidos.controller.web.ProducaoWebController;
import br.com.sergio.gestaopedidos.dto.producao.ItemProducaoRequest;
import br.com.sergio.gestaopedidos.dto.producao.ProducaoRequest;
import br.com.sergio.gestaopedidos.service.ProducaoService;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProducaoEdicaoTemplateTest {

    @Test
    void edicaoEntregaAoFormularioTodosOsValoresPersistidos() {
        ProducaoService service = mock(ProducaoService.class);
        ProducaoRequest salvo = ProducaoRequest.builder()
                .dataProducao(LocalDate.of(2026, 8, 4))
                .observacao("Panela da manhã")
                .valorGasEnergia(new BigDecimal("5.00"))
                .valorOutros(new BigDecimal("2.00"))
                .itens(List.of(ItemProducaoRequest.builder().id(8L).produtoId(1L)
                        .quantidade(new BigDecimal("35.000")).build())).build();
        when(service.buscarRascunhoParaEdicao(9L)).thenReturn(salvo);
        when(service.produtosDisponiveis()).thenReturn(List.of());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = new ProducaoWebController(service).editar(9L, model, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("producoes/formulario");
        assertThat(model.get("producao")).isSameAs(salvo);
        assertThat(((ProducaoRequest) model.get("producao")).dataProducao()).isEqualTo("2026-08-04");
        assertThat(model.get("modoEdicao")).isEqualTo(true);
    }

    @Test
    void dataUsaBindingIsoAceitoPeloInputDate() throws Exception {
        DateTimeFormat formato = ProducaoRequest.class.getDeclaredMethod("dataProducao")
                .getAnnotation(DateTimeFormat.class);
        String html = Files.readString(Path.of("src/main/resources/templates/producoes/formulario.html"));

        assertThat(formato).isNotNull();
        assertThat(formato.iso()).isEqualTo(DateTimeFormat.ISO.DATE);
        assertThat(html).contains("type=\"date\" th:field=\"*{dataProducao}\"");
        assertThat(html).doesNotContain("dd/MM/yyyy");
    }

    @Test
    void detalhesUsamTotaisCalculadosPeloBackend() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/producoes/detalhes.html"));
        assertThat(html).contains("resumo.producao.valorInsumosConsumidos",
                "resumo.producao.gastosAdicionais", "resumo.producao.custoTotal",
                "i.fatorProducao", "i.custoTotal", "i.custoUnitario");
        assertThat(html).doesNotContain("Calculado na confirmação");
    }
}
