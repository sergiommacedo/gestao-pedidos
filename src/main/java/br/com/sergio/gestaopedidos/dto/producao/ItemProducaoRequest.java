package br.com.sergio.gestaopedidos.dto.producao;
import jakarta.validation.constraints.*;import lombok.*;import java.math.BigDecimal;
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder public class ItemProducaoRequest { private Long id; @NotNull(message="Selecione o produto produzido.") private Long produtoId; @NotNull(message="Informe a quantidade produzida.") @DecimalMin(value="0.001",message="Quantidade deve ser maior que zero.") private BigDecimal quantidade; }
