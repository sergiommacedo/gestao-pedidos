package br.com.sergio.gestaopedidos.enums;

public enum UnidadeVenda {

    UNIDADE("Unidade"),
    QUILOGRAMA("Quilograma");

    private final String descricao;

    UnidadeVenda(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}