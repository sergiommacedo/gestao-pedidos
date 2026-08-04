package br.com.sergio.gestaopedidos.dto.producao;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;

@Builder
public record PreviaProducaoResponse(Long produtoId, String produtoNome,
        BigDecimal rendimentoEsperado, BigDecimal rendimentoReal, UnidadeMedida unidade,
        BigDecimal fatorProducao, List<Insumo> insumos,
        BigDecimal custoTotalEstimado, BigDecimal custoEstimadoPorUnidade) {
    @Builder
    public record Insumo(Long id, String nome, UnidadeMedida unidade,
            BigDecimal quantidadeNecessaria, BigDecimal estoqueDisponivel,
            BigDecimal custoMedio, BigDecimal valorEstimado, boolean estoqueSuficiente) {}
}
