package br.com.sergio.gestaopedidos.dto.producao;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import br.com.sergio.gestaopedidos.enums.StatusProducao;

@Builder
public record ProducaoResponse(
        Long id, LocalDate dataProducao, BigDecimal valorInsumosConsumidos,
        BigDecimal valorGasEnergia, BigDecimal valorOutros,
        BigDecimal gastosAdicionais, BigDecimal custoTotal,
        String observacao, LocalDateTime criadoEm, LocalDateTime atualizadoEm,
        StatusProducao status, LocalDateTime confirmadaEm, List<ItemProducaoResponse> itens,
        BigDecimal quantidadeTotal
) {}
