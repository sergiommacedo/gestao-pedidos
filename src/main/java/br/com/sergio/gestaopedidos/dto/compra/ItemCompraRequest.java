package br.com.sergio.gestaopedidos.dto.compra;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemCompraRequest {
    private Long id;
    @NotNull(message="Selecione o item comprado.") private Long referenciaId;
    @NotNull(message="Informe a quantidade.") @DecimalMin(value="0.001",message="Quantidade deve ser maior que zero.") @Digits(integer=12,fraction=3) private BigDecimal quantidade;
    @NotNull(message="Informe o valor pago.") @DecimalMin(value="0.01",message="Valor pago deve ser maior que zero.") @Digits(integer=13,fraction=2) private BigDecimal valorTotalItem;
}
