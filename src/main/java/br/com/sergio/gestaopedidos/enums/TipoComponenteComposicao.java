package br.com.sergio.gestaopedidos.enums;

import lombok.Getter;

@Getter
public enum TipoComponenteComposicao {
    INSUMO("Insumo"),
    PREPARACAO_PRODUZIDA("Preparação produzida");

    private final String descricao;

    TipoComponenteComposicao(String descricao) {
        this.descricao = descricao;
    }
}
