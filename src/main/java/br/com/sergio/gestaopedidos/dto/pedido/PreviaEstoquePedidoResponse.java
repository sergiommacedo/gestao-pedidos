package br.com.sergio.gestaopedidos.dto.pedido;

import br.com.sergio.gestaopedidos.enums.TipoItemEstoque;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PreviaEstoquePedidoResponse(
        boolean estoqueSuficiente,
        BigDecimal custoEstimado,
        BigDecimal receitaProdutos,
        BigDecimal lucroBrutoEstimado,
        BigDecimal margemBrutaEstimada,
        List<Componente> componentes,
        List<Item> itens
) {
    @Builder
    public record Componente(TipoItemEstoque tipo, Long referenciaId, String nome,
                             UnidadeMedida unidade, BigDecimal necessario,
                             BigDecimal disponivel, BigDecimal faltante,
                             BigDecimal custoUnitario, BigDecimal custoTotal,
                             boolean suficiente) {}

    @Builder
    public record Item(Long produtoId, String produtoNome, BigDecimal quantidade,
                       BigDecimal custoEstimado, BigDecimal lucroBrutoEstimado,
                       List<Consumo> consumos) {}

    @Builder
    public record Consumo(String nome, UnidadeMedida unidade, BigDecimal quantidade) {}
}
