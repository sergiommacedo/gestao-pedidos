package br.com.sergio.gestaopedidos.dto.insumo;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record InsumoResponse(
        Long id,
        String nome,
        String descricao,
        UnidadeMedida unidadeMedida,
        Boolean ativo,
        BigDecimal estoqueMinimo,
        String observacao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {}
