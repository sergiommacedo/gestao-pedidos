package br.com.sergio.gestaopedidos.dto.produto;

import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.enums.TipoProduto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "Dados necessários para cadastro ou atualização de um produto")
public record ProdutoRequest(

        @Schema(
                description = "Nome do produto",
                example = "Feijoada Grande",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String nome,

        @Schema(
                description = "Descrição do produto",
                example = "Feijoada completa para duas pessoas."
        )
        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres.")
        String descricao,

        @Schema(
                description = "Preço do produto",
                example = "59.90",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal preco,

        @Schema(
                description = "Indica se o produto está ativo",
                example = "true"
        )
        Boolean ativo,

        @Schema(
                description = "Indica unidade de venda",
                example = "true"
        )
        @NotNull(message = "Unidade de venda é obrigatória.")
        UnidadeVenda unidadeVenda,

        @Schema(
                description = "Informa se permite acompanhamento",
                example = "true"
        )
        @NotNull(message = "Informe se o produto permite acompanhamento.")
        Boolean permiteAcompanhamento,

        @NotNull(message = "Tipo é obrigatório.") TipoProduto tipoProduto,

        Boolean vendavel,

        @DecimalMin(value = "0.0", message = "Estoque mínimo não pode ser negativo.")
        BigDecimal estoqueMinimo

) {
}
