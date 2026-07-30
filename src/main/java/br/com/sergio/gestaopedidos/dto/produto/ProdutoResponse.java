package br.com.sergio.gestaopedidos.dto.produto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "Dados retornados de um produto")
public record ProdutoResponse(

        @Schema(description = "Identificador do produto", example = "1")
        Long id,

        @Schema(description = "Nome do produto", example = "Feijoada Grande")
        String nome,

        @Schema(description = "Descrição do produto", example = "Feijoada completa para duas pessoas.")
        String descricao,

        @Schema(description = "Preço do produto", example = "59.90")
        BigDecimal preco,

        @Schema(description = "Indica se o produto está ativo", example = "true")
        Boolean ativo

) {
}