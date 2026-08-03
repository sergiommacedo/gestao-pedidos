package br.com.sergio.gestaopedidos.dto.ficha;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record ItemFichaTecnicaResponse(Long id, Long insumoId, String insumoNome,
        UnidadeMedida unidadeMedida, BigDecimal quantidade, BigDecimal estoqueAtual,
        BigDecimal custoMedioAtual, BigDecimal custoEstimado, boolean possuiCusto) {}
