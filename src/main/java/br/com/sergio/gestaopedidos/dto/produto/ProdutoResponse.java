package br.com.sergio.gestaopedidos.dto.produto;

import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.enums.TipoProduto;
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
        Boolean ativo,

        @Schema(description = "Indica unidade de venda", example = "true")
        UnidadeVenda unidadeVenda,

        @Schema(description = "Informa se permite acompanhamento", example = "true")
        Boolean permiteAcompanhamento,

        @Schema(description = "Classificação do produto", example = "PRODUZIDO")
        TipoProduto tipoProduto,

        @Schema(description = "Indica se pode ser incluído em novos pedidos", example = "true")
        Boolean vendavel

) {
}
