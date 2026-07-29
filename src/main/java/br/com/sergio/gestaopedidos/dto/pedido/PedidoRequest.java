package br.com.sergio.gestaopedidos.dto.pedido;

import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PedidoRequest(

        @NotNull(message = "Cliente é obrigatório.")
        Long clienteId,

        @NotNull(message = "Forma de pagamento é obrigatória.")
        FormaPagamento formaPagamento,

        @NotNull(message = "Tipo de entrega é obrigatório.")
        TipoEntrega tipoEntrega,

        BigDecimal taxaEntrega,

        String observacao,

        @Valid
        @NotEmpty(message = "O pedido deve possuir pelo menos um item.")
        List<ItemPedidoRequest> itens

) {
}