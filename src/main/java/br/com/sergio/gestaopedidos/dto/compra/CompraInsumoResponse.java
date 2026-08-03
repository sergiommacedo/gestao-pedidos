package br.com.sergio.gestaopedidos.dto.compra;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
@Builder public record CompraInsumoResponse(Long id,LocalDate dataCompra,String fornecedor,String observacao,BigDecimal valorTotal,int quantidadeItens,LocalDateTime criadoEm,LocalDateTime atualizadoEm,List<ItemCompraInsumoResponse> itens){}
