package br.com.sergio.gestaopedidos.dto.ficha;

import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record FichaTecnicaResponse(Long id, Long produtoId, String produtoNome,
        UnidadeVenda unidadeVendaProduto, BigDecimal precoVenda, String observacao,
        Boolean ativa, LocalDateTime criadoEm, LocalDateTime atualizadoEm,
        List<ItemFichaTecnicaResponse> itens, BigDecimal custoEstimadoTotal,
        boolean custoCompleto, int quantidadeItensSemCusto,
        BigDecimal margemContribuicaoEstimada, BigDecimal margemPercentualEstimada) {}
