package br.com.sergio.gestaopedidos.dto.relatorio;

import java.math.BigDecimal;

public record RelatorioProducaoIndicadoresResponse(
        long produtosDistintos,
        BigDecimal totalUnidades,
        BigDecimal totalQuilogramas,
        BigDecimal faturamentoProdutos,
        BigDecimal taxasEntrega,
        BigDecimal totalGeral,
        String produtoLiderNome,
        BigDecimal produtoLiderValor
) {
    public static RelatorioProducaoIndicadoresResponse vazio() {
        return new RelatorioProducaoIndicadoresResponse(
                0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, "Nenhum", BigDecimal.ZERO
        );
    }
}
