package br.com.sergio.gestaopedidos.dto.pedido;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardOperacionalResponse(
        LocalDate data,
        long agendados,
        long entregues,
        long cancelados,
        BigDecimal valorDoDia
) {
}
