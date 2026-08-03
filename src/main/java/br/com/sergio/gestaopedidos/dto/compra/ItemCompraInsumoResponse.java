package br.com.sergio.gestaopedidos.dto.compra;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import lombok.Builder;
import java.math.BigDecimal;
@Builder public record ItemCompraInsumoResponse(Long id,Long insumoId,String insumoNome,UnidadeMedida unidadeMedida,BigDecimal quantidade,BigDecimal valorTotalItem,BigDecimal custoUnitario){}
