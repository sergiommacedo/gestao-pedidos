package br.com.sergio.gestaopedidos.dto.compra;

import br.com.sergio.gestaopedidos.enums.TipoItemEstoque;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;

import java.util.List;

public record HistoricoPrecoPageResponse(String item, TipoItemEstoque categoria, UnidadeMedida unidade,
        List<AnalisePrecosCompraResponse.HistoricoItem> conteudo, int pagina, int totalPaginas,
        long totalElementos, boolean primeira, boolean ultima) {}
