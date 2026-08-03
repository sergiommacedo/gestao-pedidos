package br.com.sergio.gestaopedidos.dto.resumo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumoVendasDiaResponse(
        LocalDate dataReferencia,
        List<ResumoProdutoVendidoResponse> produtos,
        BigDecimal totalProdutos,
        BigDecimal totalTaxasEntrega,
        BigDecimal totalGeral
) {
}
