package br.com.sergio.gestaopedidos.dto.estoque;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record MovimentacaoPedidoResponse(Long id, String produtoNome, BigDecimal quantidade,
        UnidadeMedida unidade, BigDecimal saldoAnterior, BigDecimal saldoPosterior,
        LocalDateTime dataMovimentacao) {}
