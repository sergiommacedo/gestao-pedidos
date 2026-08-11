package br.com.sergio.gestaopedidos.mapper;

import br.com.sergio.gestaopedidos.dto.pedido.ItemPedidoResponse;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.entity.ItemPedido;
import br.com.sergio.gestaopedidos.entity.Pedido;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PedidoMapper {

    @Mapping(target = "clienteId", source = "cliente.id")
    @Mapping(target = "clienteNome", source = "cliente.nome")
    @Mapping(target = "clienteTelefone", source = "cliente.telefone")
    @Mapping(target = "criadoEm", source = "dataPedido")
    @Mapping(target = "motivoCancelamento", source = "motivoCancelamento")
    @Mapping(target = "enderecoEntrega", source = "enderecoEntregaHistorico")
    @Mapping(target = "numeroEntrega", source = "numeroEntregaHistorico")
    @Mapping(target = "bairroEntrega", source = "bairroEntregaHistorico")
    @Mapping(target = "cidadeEntrega", source = "cidadeEntregaHistorico")
    @Mapping(target = "cepEntrega", source = "cepEntregaHistorico")
    @Mapping(target = "complementoEntrega", source = "complementoEntregaHistorico")
    @Mapping(target = "enderecoEntregaResumido", source = ".", qualifiedByName = "enderecoResumido")
    @Mapping(target = "enderecoEntregaCompleto", source = ".", qualifiedByName = "enderecoCompleto")
    @Mapping(target = "enderecoEntregaCompletoParaNavegacao", source = ".", qualifiedByName = "enderecoNavegavel")
    PedidoResponse toResponse(Pedido pedido);

    @Named("enderecoNavegavel")
    default boolean enderecoNavegavel(Pedido pedido) {
        return pedido.getTipoEntrega() == br.com.sergio.gestaopedidos.enums.TipoEntrega.ENTREGA
                && preenchido(pedido.getEnderecoEntregaHistorico())
                && preenchido(pedido.getNumeroEntregaHistorico())
                && preenchido(pedido.getBairroEntregaHistorico())
                && preenchido(pedido.getCidadeEntregaHistorico());
    }

    @Named("enderecoResumido")
    default String enderecoResumido(Pedido pedido) {
        if (pedido.getTipoEntrega() != br.com.sergio.gestaopedidos.enums.TipoEntrega.ENTREGA) return null;
        String logradouro = pedido.getEnderecoEntregaHistorico();
        String numero = pedido.getNumeroEntregaHistorico();
        if (!preenchido(logradouro)) return "";
        return preenchido(numero) ? logradouro + ", " + numero : logradouro;
    }

    @Named("enderecoCompleto")
    default String enderecoCompleto(Pedido pedido) {
        if (pedido.getTipoEntrega() != br.com.sergio.gestaopedidos.enums.TipoEntrega.ENTREGA) return null;
        String linha = enderecoResumido(pedido);
        return juntar(" - ", linha, pedido.getBairroEntregaHistorico(), pedido.getCidadeEntregaHistorico(),
                pedido.getCepEntregaHistorico(), pedido.getComplementoEntregaHistorico());
    }

    private boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }

    private String juntar(String separador, String... valores) {
        return java.util.Arrays.stream(valores).filter(this::preenchido)
                .collect(java.util.stream.Collectors.joining(separador));
    }

    @Mapping(target = "produtoId", source = "produto.id")
    @Mapping(target = "produtoNome", source = "produto.nome")
    @Mapping(target = "unidadeVenda", source = "produto.unidadeVenda")
    @Mapping(target = "valorUnitario", source = "precoUnitario")
    @Mapping(target = "lucroBrutoEstimado", expression = "java(itemPedido.lucroBrutoEstimado())")
    ItemPedidoResponse toItemResponse(ItemPedido itemPedido);

    List<PedidoResponse> toResponseList(List<Pedido> pedidos);
}
