package br.com.sergio.gestaopedidos.mapper;

import br.com.sergio.gestaopedidos.dto.pedido.ItemPedidoResponse;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.entity.ItemPedido;
import br.com.sergio.gestaopedidos.entity.Pedido;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PedidoMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNome", source = "cliente.nome")
    @Mapping(target = "clienteTelefone", source = "cliente.telefone")
    @Mapping(target = "criadoEm", source = "dataPedido")
    PedidoResponse toResponse(Pedido pedido);

    @Mapping(target = "produtoId", source = "produto.id")
    @Mapping(target = "produtoNome", source = "produto.nome")
    @Mapping(target = "valorUnitario", source = "precoUnitario")
    ItemPedidoResponse toItemResponse(ItemPedido itemPedido);

    List<PedidoResponse> toResponseList(List<Pedido> pedidos);
}
