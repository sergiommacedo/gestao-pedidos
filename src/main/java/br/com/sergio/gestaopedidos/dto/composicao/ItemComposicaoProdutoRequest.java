package br.com.sergio.gestaopedidos.dto.composicao;
import br.com.sergio.gestaopedidos.enums.TipoComponenteComposicao;import jakarta.validation.constraints.*;import lombok.*;import java.math.BigDecimal;
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder public class ItemComposicaoProdutoRequest{private Long id;@NotNull private TipoComponenteComposicao tipoComponente;@NotNull private Long referenciaId;@NotNull @DecimalMin("0.001") private BigDecimal quantidade;}
