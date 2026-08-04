package br.com.sergio.gestaopedidos.dto.producao;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;import lombok.Builder;import java.math.BigDecimal;
@Builder public record ItemProducaoResponse(Long id,Long produtoId,String produtoNome,UnidadeMedida unidade,BigDecimal quantidade,BigDecimal rendimentoEsperadoHistorico,BigDecimal fatorProducao,BigDecimal custoTotal,BigDecimal custoUnitario,boolean fichaAtiva){}
