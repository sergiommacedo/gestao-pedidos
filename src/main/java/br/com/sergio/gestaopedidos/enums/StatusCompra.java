package br.com.sergio.gestaopedidos.enums;

import lombok.Getter;

@Getter
public enum StatusCompra {
    ATIVA("Ativa"), ESTORNADA("Estornada");
    private final String descricao;
    StatusCompra(String descricao) { this.descricao = descricao; }
}
