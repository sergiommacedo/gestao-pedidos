package br.com.sergio.gestaopedidos.dto.compra;

import br.com.sergio.gestaopedidos.enums.*;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record ItemCompraResponse(Long id,Long referenciaId,TipoItemEstoque tipoItem,String nomeHistorico,String categoria,
                                 UnidadeMedida unidadeHistorica,BigDecimal quantidade,
                                 BigDecimal valorTotalItem,BigDecimal custoUnitario) {}
