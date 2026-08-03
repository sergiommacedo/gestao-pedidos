package br.com.sergio.gestaopedidos.dto.producao;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record ProducaoResumoResponse(
        ProducaoResponse producao, long pedidosValidos, BigDecimal faturamentoProdutos,
        BigDecimal taxasEntrega, BigDecimal faturamentoTotal, BigDecimal totalGasto,
        BigDecimal resultadoBrutoEstimado, BigDecimal margemBrutaEstimada
) {}
