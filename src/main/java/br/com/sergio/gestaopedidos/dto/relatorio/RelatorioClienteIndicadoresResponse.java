package br.com.sergio.gestaopedidos.dto.relatorio;

import java.math.BigDecimal;

public record RelatorioClienteIndicadoresResponse(
        long clientesCompradores,
        long pedidosValidos,
        BigDecimal faturamentoTotal,
        BigDecimal ticketMedioGeral,
        long clientesRecorrentes,
        String clienteLiderNome,
        BigDecimal clienteLiderValor
) {
    public static RelatorioClienteIndicadoresResponse vazio() {
        return new RelatorioClienteIndicadoresResponse(
                0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0, "Nenhum", BigDecimal.ZERO
        );
    }
}
