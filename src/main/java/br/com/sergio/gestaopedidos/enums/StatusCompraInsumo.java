package br.com.sergio.gestaopedidos.enums;
import lombok.Getter;
@Getter public enum StatusCompraInsumo{ATIVA("Ativa"),ESTORNADA("Estornada");private final String descricao;StatusCompraInsumo(String d){descricao=d;}}
