package br.com.sergio.gestaopedidos.dto.pedido;

import java.util.List;

public record PlanejamentoEntregasResponse(
        List<PlanejamentoEntregaResponse> elegiveis,
        List<PlanejamentoEntregaResponse> emRota,
        long quantidadeElegiveis,
        long quantidadeEnderecosNavegaveis,
        long quantidadeEnderecosIncompletos,
        long quantidadeEmRota
) {
}
