package br.com.sergio.gestaopedidos.dto.composicao;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import lombok.*;import java.util.*;
@Getter@Setter@NoArgsConstructor@AllArgsConstructor@Builder public class ComposicaoProdutoRequest{@NotNull private Long produtoComercialId;private Boolean ativa;@Size(max=500)private String observacao;@Valid @NotEmpty @Builder.Default private List<ItemComposicaoProdutoRequest> itens=new ArrayList<>();}
