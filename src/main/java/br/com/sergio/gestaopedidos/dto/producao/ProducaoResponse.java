package br.com.sergio.gestaopedidos.dto.producao;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.*;

@Builder
public record ProducaoResponse(
        Long id, LocalDate dataProducao, BigDecimal saldoInicialMateriais, BigDecimal valorComprasMateriais,
        BigDecimal saldoFinalMateriais, BigDecimal recursosDisponiveis, BigDecimal custoMateriaisConsumidos,
        BigDecimal valorEmbalagens, BigDecimal valorGasEnergia, BigDecimal valorOutros,
        BigDecimal outrosCustos, BigDecimal totalGasto,
        String observacao, LocalDateTime criadoEm, LocalDateTime atualizadoEm
) {}
