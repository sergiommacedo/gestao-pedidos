package br.com.sergio.gestaopedidos.dto.compra;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import br.com.sergio.gestaopedidos.enums.StatusCompraInsumo;
@Builder public record CompraInsumoResponse(Long id,LocalDate dataCompra,String fornecedor,String observacao,BigDecimal valorTotal,int quantidadeItens,LocalDateTime criadoEm,LocalDateTime atualizadoEm,StatusCompraInsumo status,List<ItemCompraInsumoResponse> itens){}
