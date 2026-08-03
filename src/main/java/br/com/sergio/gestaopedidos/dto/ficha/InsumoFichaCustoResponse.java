package br.com.sergio.gestaopedidos.dto.ficha;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record InsumoFichaCustoResponse(Long id, String nome, UnidadeMedida unidade,
        BigDecimal custoMedio, BigDecimal estoqueAtual, boolean possuiCusto) {}
