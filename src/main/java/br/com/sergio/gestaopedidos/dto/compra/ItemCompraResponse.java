package br.com.sergio.gestaopedidos.dto.compra;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record ItemCompraResponse(Long id,Long referenciaId,String nomeHistorico,String categoria,
                                 UnidadeMedida unidadeHistorica,BigDecimal quantidade,
                                 BigDecimal valorTotalItem,BigDecimal custoUnitario) {}
