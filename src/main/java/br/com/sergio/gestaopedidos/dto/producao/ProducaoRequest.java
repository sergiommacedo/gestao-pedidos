package br.com.sergio.gestaopedidos.dto.producao;

import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;

@Builder
public record ProducaoRequest(
        @NotNull(message = "Data da produção é obrigatória.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataProducao,
        @DecimalMin(value = "0.00", message = "Gás/Energia não pode ser negativo.") BigDecimal valorGasEnergia,
        @DecimalMin(value = "0.00", message = "Outros não pode ser negativo.") BigDecimal valorOutros,
        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres.") String observacao,
        @Valid @NotEmpty(message = "Adicione ao menos uma preparação produzida.") List<ItemProducaoRequest> itens
) {}
