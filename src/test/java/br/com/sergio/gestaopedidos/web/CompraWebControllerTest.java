package br.com.sergio.gestaopedidos.web;

import br.com.sergio.gestaopedidos.controller.web.CompraWebController;
import br.com.sergio.gestaopedidos.dto.insumo.InsumoResponse;
import br.com.sergio.gestaopedidos.dto.produto.ProdutoResponse;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.service.*;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CompraWebControllerTest {
    @Test void buscaUnificadaRetornaSomenteResultadosDosServicosPermitidosESeparaIdsIguais(){
        CompraService compras=mock(CompraService.class);InsumoService insumos=mock(InsumoService.class);ProdutoService produtos=mock(ProdutoService.class);
        when(insumos.buscarAtivosPorNome("fra")).thenReturn(List.of(InsumoResponse.builder().id(15L).nome("Fralda").unidadeMedida(UnidadeMedida.UNIDADE).build()));
        when(produtos.buscarRevendaAtivosPorNome("fra")).thenReturn(List.of(ProdutoResponse.builder().id(15L).nome("Frango Assado").tipoProduto(TipoProduto.PRODUTO_REVENDA).unidadeVenda(UnidadeVenda.UNIDADE).build()));
        var resultado=new CompraWebController(compras,insumos,produtos).buscarItens("fra");
        assertThat(resultado).extracting(i->i.tipoItem()+":"+i.id()).containsExactly("INSUMO:15","PRODUTO_REVENDA:15");
        verify(insumos).buscarAtivosPorNome("fra");verify(produtos).buscarRevendaAtivosPorNome("fra");
    }

    @Test void formularioRemoveCategoriaPreviaEMantemSubmitExplicito()throws Exception{
        String html=Files.readString(Path.of("src/main/resources/templates/compras/formulario.html"));
        assertThat(html).doesNotContain("O que foi comprado?","data-tipo-compra","th:field=\"*{tipoCompra}\"");
        assertThat(html).contains("data-tipo-item","data-salvar-compra","type=\"submit\"");
    }

    @Test void javascriptBloqueiaEnterUsaChaveCompostaEPriorizaNovoItem()throws Exception{
        String js=Files.readString(Path.of("src/main/resources/static/js/app.js"));
        assertThat(js).contains("`${item.tipoItem}:${item.referenciaId}`","container.prepend(linha)","evento.submitter?.matches(\"[data-salvar-compra]\")","quantidade.focus()","busca.value = \"\"");
        assertThat(js).doesNotContain("/compras/itens/buscar?tipo=");
    }
}
