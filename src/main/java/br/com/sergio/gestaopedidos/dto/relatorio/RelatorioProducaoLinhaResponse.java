package br.com.sergio.gestaopedidos.dto.relatorio;

import br.com.sergio.gestaopedidos.enums.UnidadeVenda;

import java.math.BigDecimal;

public record RelatorioProducaoLinhaResponse(
        Long produtoId,
        String produtoNome,
        UnidadeVenda unidadeVenda,
        BigDecimal quantidadeTotal,
        long pedidosDistintos,
        BigDecimal faturamentoTotal,
        BigDecimal mediaPorPedido,
        BigDecimal participacaoPercentual,
        long posicao
) {
}
