package br.com.sergio.gestaopedidos.dto.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RelatorioClienteLinhaResponse(
        Long clienteId,
        String clienteNome,
        String clienteTelefone,
        Long pedidosValidos,
        BigDecimal faturamentoTotal,
        BigDecimal ticketMedio,
        LocalDate primeiraCompra,
        LocalDate ultimaCompra,
        Long entregas,
        Long retiradas,
        BigDecimal participacaoPercentual,
        Long posicao
) {
}
