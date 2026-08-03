package br.com.sergio.gestaopedidos.dto.compra;

import br.com.sergio.gestaopedidos.enums.TipoCompra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompraRequest {
    @NotNull(message="Escolha o tipo da compra.") private TipoCompra tipoCompra;
    @NotNull(message="Data da compra é obrigatória.") private LocalDate dataCompra;
    @Size(max=150) private String fornecedor;
    @Size(max=500) private String observacao;
    @Valid @NotEmpty(message="Adicione ao menos um item à compra.") @Builder.Default private List<ItemCompraRequest> itens=new ArrayList<>();
}
