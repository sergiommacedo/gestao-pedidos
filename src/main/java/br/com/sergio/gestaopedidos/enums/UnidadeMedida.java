package br.com.sergio.gestaopedidos.enums;

import lombok.Getter;

@Getter
public enum UnidadeMedida {
    UNIDADE("Unidade", "un"),
    QUILOGRAMA("Quilograma", "kg"),
    GRAMA("Grama", "g"),
    LITRO("Litro", "L"),
    MILILITRO("Mililitro", "ml");

    private final String descricao;
    private final String simbolo;

    UnidadeMedida(String descricao, String simbolo) {
        this.descricao = descricao;
        this.simbolo = simbolo;
    }
}
