package br.com.sergio.gestaopedidos.dto.producao;
import br.com.sergio.gestaopedidos.enums.*;import lombok.Builder;import java.math.BigDecimal;import java.time.LocalDateTime;
@Builder public record MovimentacaoProducaoResponse(Long id,LocalDateTime data,String item,TipoItemEstoque categoria,TipoMovimentacaoEstoque tipo,UnidadeMedida unidade,BigDecimal quantidade,BigDecimal saldoAnterior,BigDecimal saldoPosterior){}
