package br.com.sergio.gestaopedidos.dto.composicao;
import br.com.sergio.gestaopedidos.enums.*;import lombok.Builder;import java.math.BigDecimal;
@Builder public record ItemComposicaoProdutoResponse(Long id,TipoComponenteComposicao tipoComponente,Long referenciaId,String nome,UnidadeMedida unidade,BigDecimal quantidade,BigDecimal custoMedio,BigDecimal custoEstimado){}
