package br.com.sergio.gestaopedidos.dto.pedido;

import br.com.sergio.gestaopedidos.enums.StatusPedido;

import java.time.LocalTime;

public record PlanejamentoEntregaResponse(
        Long id,
        String clienteNome,
        String clienteTelefone,
        LocalTime horarioInicio,
        LocalTime horarioFim,
        StatusPedido status,
        String enderecoResumido,
        String enderecoCompleto,
        String bairro,
        boolean enderecoNavegavel,
        boolean jaEmRota,
        boolean planejamentoConfirmado,
        Integer ordemPlanejada
) {
}
