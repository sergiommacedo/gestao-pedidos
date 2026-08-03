package br.com.sergio.gestaopedidos.dto.ficha;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemFichaTecnicaRequest {
    private Long id;
    @NotNull(message = "Selecione o insumo.")
    private Long insumoId;
    @NotNull(message = "Informe a quantidade.")
    @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero.")
    @Digits(integer = 12, fraction = 3, message = "Use no máximo três casas decimais.")
    private BigDecimal quantidade;
}
