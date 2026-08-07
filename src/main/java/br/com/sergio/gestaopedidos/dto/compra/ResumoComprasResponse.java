package br.com.sergio.gestaopedidos.dto.compra;

import java.math.BigDecimal;

public record ResumoComprasResponse(long comprasHoje, BigDecimal totalHoje, BigDecimal totalMes,
                                    BigDecimal totalPeriodo, boolean periodoFiltrado) {}
