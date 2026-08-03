package br.com.sergio.gestaopedidos.dto.ficha;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FichaTecnicaRequest {
    @NotNull(message = "Selecione o produto.")
    private Long produtoId;
    @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
    private String observacao;
    @Builder.Default
    private Boolean ativa = true;
    @Valid @NotEmpty(message = "Adicione ao menos um insumo à ficha técnica.")
    @Builder.Default
    private List<ItemFichaTecnicaRequest> itens = new ArrayList<>();
}
