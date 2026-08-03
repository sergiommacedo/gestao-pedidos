package br.com.sergio.gestaopedidos.dto.compra;

import br.com.sergio.gestaopedidos.enums.*;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Builder
public record CompraResponse(Long id,TipoCompra tipoCompra,LocalDate dataCompra,String fornecedor,String observacao,
                             BigDecimal valorTotal,int quantidadeItens,LocalDateTime criadoEm,
                             LocalDateTime atualizadoEm,StatusCompra status,List<ItemCompraResponse> itens) {}
