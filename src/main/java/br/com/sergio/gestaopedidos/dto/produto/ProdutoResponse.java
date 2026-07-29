package br.com.sergio.gestaopedidos.dto.produto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProdutoResponse(

        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Boolean ativo

) {
}