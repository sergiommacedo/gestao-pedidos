package br.com.sergio.gestaopedidos.dto.estoque;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EntradaEstoqueRequest {
    @NotNull(message = "Selecione o insumo.") private Long insumoId;
    @NotNull(message = "Informe a quantidade.") @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero.") private BigDecimal quantidade;
    @NotNull(message = "Informe o valor total.") @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero.") private BigDecimal valorTotal;
    @NotNull(message = "Data é obrigatória.") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") private LocalDateTime dataMovimentacao;
    @Size(max = 500) private String observacao;
}
