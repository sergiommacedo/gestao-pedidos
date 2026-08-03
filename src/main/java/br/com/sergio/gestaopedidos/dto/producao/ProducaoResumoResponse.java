package br.com.sergio.gestaopedidos.dto.producao;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record ProducaoResumoResponse(
        ProducaoResponse producao, BigDecimal custoTotal
) {}
