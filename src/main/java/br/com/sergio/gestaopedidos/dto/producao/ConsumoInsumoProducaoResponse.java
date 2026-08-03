package br.com.sergio.gestaopedidos.dto.producao;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;import lombok.Builder;import java.math.BigDecimal;
@Builder public record ConsumoInsumoProducaoResponse(Long insumoId,String insumoNome,UnidadeMedida unidade,BigDecimal quantidade,BigDecimal custoMedio,BigDecimal custoTotal){}
