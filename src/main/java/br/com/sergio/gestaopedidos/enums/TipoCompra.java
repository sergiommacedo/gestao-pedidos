package br.com.sergio.gestaopedidos.enums;

import lombok.Getter;

@Getter
public enum TipoCompra {
    INSUMO("Insumos"),
    PRODUTO_REVENDA("Produtos de revenda"),
    MISTA("Insumos e produtos de revenda");

    private final String descricao;
    TipoCompra(String descricao) { this.descricao = descricao; }
}
