package br.com.sergio.gestaopedidos.dto.producao;

import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;

@Builder
public record ProducaoRequest(
        @NotNull(message = "Data da produção é obrigatória.") LocalDate dataProducao,
        @DecimalMin(value = "0.00", message = "Saldo inicial de materiais não pode ser negativo.") BigDecimal saldoInicialMateriais,
        @DecimalMin(value = "0.00", message = "Compras da produção não pode ser negativo.") BigDecimal valorComprasMateriais,
        @DecimalMin(value = "0.00", message = "Saldo final de materiais não pode ser negativo.") BigDecimal saldoFinalMateriais,
        @DecimalMin(value = "0.00", message = "Embalagens não pode ser negativo.") BigDecimal valorEmbalagens,
        @DecimalMin(value = "0.00", message = "Gás/Energia não pode ser negativo.") BigDecimal valorGasEnergia,
        @DecimalMin(value = "0.00", message = "Outros não pode ser negativo.") BigDecimal valorOutros,
        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres.") String observacao,
        @Valid @NotEmpty(message = "Adicione ao menos um produto produzido.") List<ItemProducaoRequest> itens
) {}
