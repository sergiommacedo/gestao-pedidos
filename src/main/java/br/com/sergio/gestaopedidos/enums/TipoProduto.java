package br.com.sergio.gestaopedidos.enums;

public enum TipoProduto {
    PRODUZIDO("Produzido pela empresa"),
    REVENDA("Produto de revenda");

    private final String descricao;

    TipoProduto(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
