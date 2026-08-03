package br.com.sergio.gestaopedidos.enums;
import lombok.Getter;
@Getter public enum TipoItemEstoque {INSUMO("Insumo"),PREPARACAO_PRODUZIDA("Preparação produzida"),PRODUTO_REVENDA("Produto de revenda");private final String descricao;TipoItemEstoque(String descricao){this.descricao=descricao;}}
