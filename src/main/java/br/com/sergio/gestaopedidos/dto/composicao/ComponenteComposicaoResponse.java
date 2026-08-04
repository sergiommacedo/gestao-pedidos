package br.com.sergio.gestaopedidos.dto.composicao;
import br.com.sergio.gestaopedidos.enums.*;import lombok.Builder;import java.math.BigDecimal;
@Builder public record ComponenteComposicaoResponse(TipoComponenteComposicao tipo,Long id,String nome,UnidadeMedida unidade,BigDecimal custoMedio){}
