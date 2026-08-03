package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.dto.resumo.ResumoProdutoVendidoResponse;
import br.com.sergio.gestaopedidos.entity.ItemPedido;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    @Query("""
            SELECT new br.com.sergio.gestaopedidos.dto.resumo.ResumoProdutoVendidoResponse(
                produto.id,
                produto.nome,
                produto.unidadeVenda,
                SUM(item.quantidade),
                SUM(item.subtotal)
            )
            FROM ItemPedido item
            JOIN item.pedido pedido
            JOIN item.produto produto
            WHERE pedido.dataAgendada = :dataAgendada
            AND pedido.status <> :statusExcluido
            GROUP BY produto.id, produto.nome, produto.unidadeVenda
            ORDER BY produto.nome
            """)
    List<ResumoProdutoVendidoResponse> resumirProdutosVendidosPorDataExcetoStatus(
            @Param("dataAgendada") LocalDate dataAgendada,
            @Param("statusExcluido") StatusPedido statusExcluido
    );
}
