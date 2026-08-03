package br.com.sergio.gestaopedidos.dto.resumo;

import br.com.sergio.gestaopedidos.enums.UnidadeVenda;

import java.math.BigDecimal;

public record ResumoProdutoVendidoResponse(
        Long produtoId,
        String produtoNome,
        UnidadeVenda unidadeVenda,
        BigDecimal quantidadeTotal,
        BigDecimal valorTotal
) {
}
