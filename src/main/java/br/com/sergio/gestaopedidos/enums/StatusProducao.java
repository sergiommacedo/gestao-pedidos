package br.com.sergio.gestaopedidos.enums;
import lombok.Getter;
@Getter public enum StatusProducao { RASCUNHO("Rascunho"), CONFIRMADA("Confirmada"); private final String descricao; StatusProducao(String d){descricao=d;} }
