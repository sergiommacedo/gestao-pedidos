package br.com.sergio.gestaopedidos.dto.composicao;
import lombok.Builder;import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.List;
@Builder public record ComposicaoProdutoResponse(Long id,Long produtoComercialId,String produtoComercialNome,Boolean ativa,String observacao,LocalDateTime criadoEm,LocalDateTime atualizadoEm,List<ItemComposicaoProdutoResponse> itens,BigDecimal custoEstimadoTotal){}
