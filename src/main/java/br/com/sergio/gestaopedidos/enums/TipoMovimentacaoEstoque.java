package br.com.sergio.gestaopedidos.enums;
import lombok.Getter;
@Getter public enum TipoMovimentacaoEstoque{
 ENTRADA_COMPRA("Entrada por compra",true),ENTRADA_AJUSTE("Entrada por ajuste",true),ENTRADA_PRODUCAO("Entrada por produção",true),SAIDA_CONSUMO_PRODUCAO("Consumo da produção",false),SAIDA_CONSUMO_MANUAL("Saída manual",false),SAIDA_PERDA("Perda",false),SAIDA_AJUSTE("Ajuste de saída",false),ESTORNO_ENTRADA("Estorno de entrada",false),ESTORNO_SAIDA("Estorno de saída",true);
 private final String descricao;private final boolean entrada;TipoMovimentacaoEstoque(String d,boolean e){descricao=d;entrada=e;}
}
