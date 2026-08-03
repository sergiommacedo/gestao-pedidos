package br.com.sergio.gestaopedidos.dto.compra;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.util.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompraInsumoRequest {
    @NotNull(message="Data da compra é obrigatória.") private LocalDate dataCompra;
    @Size(max=150,message="Fornecedor deve ter no máximo 150 caracteres.") private String fornecedor;
    @Size(max=500,message="Observação deve ter no máximo 500 caracteres.") private String observacao;
    @Valid @NotEmpty(message="Adicione ao menos um item à compra.") @Builder.Default private List<ItemCompraInsumoRequest> itens=new ArrayList<>();
}
