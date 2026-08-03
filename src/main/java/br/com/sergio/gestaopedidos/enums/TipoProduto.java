package br.com.sergio.gestaopedidos.enums;

public enum TipoProduto {
    PREPARACAO_PRODUZIDA("Preparação produzida"),
    PRODUTO_COMERCIAL("Produto de venda"),
    PRODUTO_REVENDA("Produto de revenda");

    private final String descricao;

    TipoProduto(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
