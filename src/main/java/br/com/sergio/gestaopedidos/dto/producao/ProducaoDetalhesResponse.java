package br.com.sergio.gestaopedidos.dto.producao;
import lombok.Builder;import java.math.BigDecimal;import java.util.List;
@Builder public record ProducaoDetalhesResponse(ProducaoResumoResponse resumo,List<ItemProducaoResponse> produtos,List<ConsumoInsumoProducaoResponse> consumos,List<MovimentacaoProducaoResponse> movimentacoes,int produtosDistintos,int insumosDistintos,BigDecimal quantidadeTotal){}
