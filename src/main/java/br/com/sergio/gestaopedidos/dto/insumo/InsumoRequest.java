package br.com.sergio.gestaopedidos.dto.insumo;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record InsumoRequest(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String nome,

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres.")
        String descricao,

        @NotNull(message = "Unidade de medida é obrigatória.")
        UnidadeMedida unidadeMedida,

        Boolean ativo,

        @DecimalMin(value = "0.000", message = "Estoque mínimo não pode ser negativo.")
        @Digits(integer = 12, fraction = 3, message = "Estoque mínimo deve ter no máximo três casas decimais.")
        BigDecimal estoqueMinimo,

        @Size(max = 500, message = "Observação deve ter no máximo 500 caracteres.")
        String observacao
) {}
