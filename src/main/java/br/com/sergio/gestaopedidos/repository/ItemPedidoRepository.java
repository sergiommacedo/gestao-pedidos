package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.dto.resumo.ResumoProdutoVendidoResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioProducaoLinhaResponse;
import br.com.sergio.gestaopedidos.entity.ItemPedido;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    interface IndicadoresRelatorioProducao {
        Long getProdutosDistintos();
        java.math.BigDecimal getTotalUnidades();
        java.math.BigDecimal getTotalQuilogramas();
        java.math.BigDecimal getFaturamentoProdutos();
    }

    interface ProdutoLiderRelatorioProducao {
        String getProdutoNome();
        java.math.BigDecimal getFaturamentoTotal();
    }

    @Query("""
            SELECT new br.com.sergio.gestaopedidos.dto.resumo.ResumoProdutoVendidoResponse(
                produto.id,
                produto.nome,
                produto.unidadeVenda,
                SUM(item.quantidade),
                SUM(item.subtotal * (1 - COALESCE(pedido.percentualDescontoGeral, 0) / 100))
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

    @Query("""
            SELECT new br.com.sergio.gestaopedidos.dto.resumo.ResumoProdutoVendidoResponse(
                produto.id, produto.nome, produto.unidadeVenda, SUM(item.quantidade), SUM(item.subtotal * (1 - COALESCE(pedido.percentualDescontoGeral, 0) / 100)))
            FROM ItemPedido item JOIN item.pedido pedido JOIN item.produto produto
            WHERE pedido.dataAgendada = :dataAgendada AND pedido.status <> :statusExcluido
            GROUP BY produto.id, produto.nome, produto.unidadeVenda
            ORDER BY SUM(item.subtotal * (1 - COALESCE(pedido.percentualDescontoGeral, 0) / 100)) DESC, produto.nome ASC
            """)
    List<ResumoProdutoVendidoResponse> resumirProdutosMaisVendidosDashboard(
            @Param("dataAgendada") LocalDate dataAgendada,
            @Param("statusExcluido") StatusPedido statusExcluido,
            Pageable pageable
    );

    String FILTROS_RELATORIO_PRODUCAO = """
            WHERE pedido.dataAgendada BETWEEN :dataInicial AND :dataFinal
            AND pedido.status <> :statusCancelado
            AND (:produto = '' OR LOWER(produto.nome) LIKE LOWER(CONCAT('%', :produto, '%')))
            AND (:unidadeVenda IS NULL OR produto.unidadeVenda = :unidadeVenda)
            AND (:tipoEntrega IS NULL OR pedido.tipoEntrega = :tipoEntrega)
            AND (:formaPagamento IS NULL OR pedido.formaPagamento = :formaPagamento)
            """;

    String SELECT_RELATORIO_PRODUCAO = """
            SELECT new br.com.sergio.gestaopedidos.dto.relatorio.RelatorioProducaoLinhaResponse(
                produto.id,
                produto.nome,
                produto.unidadeVenda,
                SUM(item.quantidade),
                COUNT(DISTINCT pedido.id),
                SUM(item.subtotal),
                SUM(item.subtotal) / COUNT(DISTINCT pedido.id),
                CASE WHEN SUM(SUM(item.subtotal)) OVER () = 0 THEN 0
                     ELSE SUM(item.subtotal) * 100 / SUM(SUM(item.subtotal)) OVER () END,
                ROW_NUMBER() OVER (ORDER BY SUM(item.subtotal) DESC, produto.nome ASC)
            )
            FROM ItemPedido item
            JOIN item.pedido pedido
            JOIN item.produto produto
            """;

    String GROUP_RELATORIO_PRODUCAO = """
            GROUP BY produto.id, produto.nome, produto.unidadeVenda
            """;

    String COUNT_RELATORIO_PRODUCAO = """
            SELECT COUNT(DISTINCT produto.id)
            FROM ItemPedido item
            JOIN item.pedido pedido
            JOIN item.produto produto
            """;

    @Query(
            value = SELECT_RELATORIO_PRODUCAO + FILTROS_RELATORIO_PRODUCAO + GROUP_RELATORIO_PRODUCAO,
            countQuery = COUNT_RELATORIO_PRODUCAO + FILTROS_RELATORIO_PRODUCAO
    )
    Page<RelatorioProducaoLinhaResponse> buscarRelatorioProducao(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("produto") String produto,
            @Param("unidadeVenda") UnidadeVenda unidadeVenda,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("statusCancelado") StatusPedido statusCancelado,
            Pageable pageable
    );

    @Query(SELECT_RELATORIO_PRODUCAO + FILTROS_RELATORIO_PRODUCAO + GROUP_RELATORIO_PRODUCAO)
    Slice<RelatorioProducaoLinhaResponse> buscarLoteRelatorioProducao(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("produto") String produto,
            @Param("unidadeVenda") UnidadeVenda unidadeVenda,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("statusCancelado") StatusPedido statusCancelado,
            Pageable pageable
    );

    @Query("""
            SELECT
                COUNT(DISTINCT produto.id) AS produtosDistintos,
                COALESCE(SUM(CASE WHEN produto.unidadeVenda = :unidade THEN item.quantidade ELSE 0 END), 0) AS totalUnidades,
                COALESCE(SUM(CASE WHEN produto.unidadeVenda = :quilograma THEN item.quantidade ELSE 0 END), 0) AS totalQuilogramas,
                COALESCE(SUM(item.subtotal), 0) AS faturamentoProdutos
            FROM ItemPedido item
            JOIN item.pedido pedido
            JOIN item.produto produto
            """ + FILTROS_RELATORIO_PRODUCAO)
    IndicadoresRelatorioProducao buscarIndicadoresRelatorioProducao(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("produto") String produto,
            @Param("unidadeVenda") UnidadeVenda unidadeVendaFiltro,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("statusCancelado") StatusPedido statusCancelado,
            @Param("unidade") UnidadeVenda unidade,
            @Param("quilograma") UnidadeVenda quilograma
    );

    @Query("""
            SELECT produto.nome AS produtoNome, SUM(item.subtotal) AS faturamentoTotal
            FROM ItemPedido item
            JOIN item.pedido pedido
            JOIN item.produto produto
            """ + FILTROS_RELATORIO_PRODUCAO + """
            GROUP BY produto.id, produto.nome
            ORDER BY SUM(item.subtotal) DESC, produto.nome ASC
            """)
    List<ProdutoLiderRelatorioProducao> buscarProdutoLiderRelatorioProducao(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("produto") String produto,
            @Param("unidadeVenda") UnidadeVenda unidadeVenda,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("statusCancelado") StatusPedido statusCancelado,
            Pageable pageable
    );
}
