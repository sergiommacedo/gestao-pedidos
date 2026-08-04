package br.com.sergio.gestaopedidos.dto.producao;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record EstoquePreparacaoProducaoResponse(
        Long produtoId,
        String produtoNome,
        UnidadeMedida unidade,
        BigDecimal estoqueAntes,
        BigDecimal producaoAdicionada,
        BigDecimal estoqueDepois,
        BigDecimal custoMedioAntes,
        BigDecimal custoMedioDepois,
        BigDecimal valorEstoqueAntes,
        BigDecimal valorEstoqueDepois,
        BigDecimal custoUnitarioProducao,
        BigDecimal valorProduzido,
        boolean historico
) {}
