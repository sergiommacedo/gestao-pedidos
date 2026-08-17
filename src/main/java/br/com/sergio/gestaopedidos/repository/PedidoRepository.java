package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Pedido;
import br.com.sergio.gestaopedidos.dto.dashboard.DashboardPedidoAtencaoResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoLinhaResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioClienteLinhaResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioFinanceiroDiaResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioFinanceiroEntregaResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioFinanceiroPagamentoResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.Collection;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    interface ResumoDashboard {
        Long getTotal(); Long getValidos(); Long getCancelados(); Long getEmPreparacao();
        Long getSaiuParaEntrega(); BigDecimal getProdutos(); BigDecimal getTaxas(); BigDecimal getFaturamento();
    }

    interface ResumoFinanceiroDashboard {
        Long getPedidosValidos(); Long getPedidosComCusto();
        BigDecimal getReceitaProdutos(); BigDecimal getCmv(); BigDecimal getLucroBruto();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"cliente", "itens", "itens.produto"})
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> bloquearDetalhado(@Param("id") Long id);

    @EntityGraph(attributePaths = {"cliente", "itens", "itens.produto"})
    @Query("SELECT p FROM Pedido p WHERE p.id = :id")
    Optional<Pedido> buscarDetalhado(@Param("id") Long id);

    interface ResumoFinanceiroProducao {
        LocalDate getDataProducao();
        Long getPedidosValidos();
        BigDecimal getFaturamentoProdutos();
        BigDecimal getTaxasEntrega();
        BigDecimal getFaturamentoTotal();
    }

    @Query("""
            SELECT p.dataAgendada AS dataProducao,
                   COUNT(p) AS pedidosValidos,
                   COALESCE(SUM(p.subtotal - COALESCE(p.valorDescontoGeral, 0)), 0) AS faturamentoProdutos,
                   COALESCE(SUM(p.taxaEntrega), 0) AS taxasEntrega,
                   COALESCE(SUM(p.valorTotal), 0) AS faturamentoTotal
            FROM Pedido p
            WHERE p.dataAgendada = :dataProducao
            AND p.status <> :statusCancelado
            GROUP BY p.dataAgendada
            """)
    Optional<ResumoFinanceiroProducao> resumirFinanceiroProducao(
            @Param("dataProducao") LocalDate dataProducao,
            @Param("statusCancelado") StatusPedido statusCancelado);

    @Query("""
            SELECT p.dataAgendada AS dataProducao,
                   COUNT(p) AS pedidosValidos,
                   COALESCE(SUM(p.subtotal - COALESCE(p.valorDescontoGeral, 0)), 0) AS faturamentoProdutos,
                   COALESCE(SUM(p.taxaEntrega), 0) AS taxasEntrega,
                   COALESCE(SUM(p.valorTotal), 0) AS faturamentoTotal
            FROM Pedido p
            WHERE p.dataAgendada IN :datas
            AND p.status <> :statusCancelado
            GROUP BY p.dataAgendada
            """)
    List<ResumoFinanceiroProducao> resumirFinanceiroProducoes(
            @Param("datas") Collection<LocalDate> datas,
            @Param("statusCancelado") StatusPedido statusCancelado);

    interface ContagemPedidosPorStatus {

        StatusPedido getStatus();

        long getQuantidade();
    }

    interface IndicadoresRelatorioPedidos {

        Long getTotalPedidos();

        Long getPedidosValidos();

        Long getCancelados();

        BigDecimal getFaturamento();

        BigDecimal getTaxasEntrega();
    }

    interface IndicadoresRelatorioClientes {
        Long getClientesCompradores();
        Long getPedidosValidos();
        BigDecimal getFaturamentoTotal();
        Long getClientesRecorrentes();
    }

    interface ClienteLiderRelatorio {
        String getClienteNome();
        BigDecimal getFaturamentoTotal();
    }

    interface IndicadoresRelatorioFinanceiro {
        Long getPedidosTotais();
        Long getPedidosValidos();
        Long getCancelados();
        BigDecimal getFaturamentoProdutos();
        BigDecimal getTaxasEntrega();
        BigDecimal getFaturamentoBruto();
        BigDecimal getValorCancelado();
    }

    interface MelhorDiaRelatorioFinanceiro {
        LocalDate getData();
        BigDecimal getFaturamento();
    }

    String FILTROS_RELATORIO_FINANCEIRO = """
            WHERE pedido.dataAgendada BETWEEN :dataInicial AND :dataFinal
            AND (:formaPagamento IS NULL OR pedido.formaPagamento = :formaPagamento)
            AND (:tipoEntrega IS NULL OR pedido.tipoEntrega = :tipoEntrega)
            AND (:status IS NULL OR pedido.status = :status)
            AND (
                :cliente = ''
                OR LOWER(cliente.nome) LIKE LOWER(CONCAT('%', :cliente, '%'))
                OR cliente.telefone LIKE CONCAT('%', :cliente, '%')
            )
            """;

    @Query("""
            SELECT
                COUNT(pedido) AS pedidosTotais,
                COALESCE(SUM(CASE WHEN pedido.status <> :statusCancelado THEN 1 ELSE 0 END), 0) AS pedidosValidos,
                COALESCE(SUM(CASE WHEN pedido.status = :statusCancelado THEN 1 ELSE 0 END), 0) AS cancelados,
                COALESCE(SUM(CASE WHEN pedido.status <> :statusCancelado THEN pedido.subtotal - COALESCE(pedido.valorDescontoGeral, 0) ELSE 0 END), 0) AS faturamentoProdutos,
                COALESCE(SUM(CASE WHEN pedido.status <> :statusCancelado THEN pedido.taxaEntrega ELSE 0 END), 0) AS taxasEntrega,
                COALESCE(SUM(CASE WHEN pedido.status <> :statusCancelado THEN pedido.valorTotal ELSE 0 END), 0) AS faturamentoBruto,
                COALESCE(SUM(CASE WHEN pedido.status = :statusCancelado THEN pedido.valorTotal ELSE 0 END), 0) AS valorCancelado
            FROM Pedido pedido
            JOIN pedido.cliente cliente
            """ + FILTROS_RELATORIO_FINANCEIRO)
    IndicadoresRelatorioFinanceiro buscarIndicadoresRelatorioFinanceiro(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("status") StatusPedido status,
            @Param("cliente") String cliente,
            @Param("statusCancelado") StatusPedido statusCancelado
    );

    String SELECT_DIAS_RELATORIO_FINANCEIRO = """
            SELECT new br.com.sergio.gestaopedidos.dto.relatorio.RelatorioFinanceiroDiaResponse(
                pedido.dataAgendada,
                SUM(CASE WHEN pedido.status <> :statusCancelado THEN 1 ELSE 0 END),
                SUM(CASE WHEN pedido.status = :statusCancelado THEN 1 ELSE 0 END),
                SUM(CASE WHEN pedido.status <> :statusCancelado THEN pedido.subtotal - COALESCE(pedido.valorDescontoGeral, 0) ELSE 0 END),
                SUM(CASE WHEN pedido.status <> :statusCancelado THEN pedido.taxaEntrega ELSE 0 END),
                SUM(CASE WHEN pedido.status <> :statusCancelado THEN pedido.valorTotal ELSE 0 END),
                CASE WHEN SUM(CASE WHEN pedido.status <> :statusCancelado THEN 1 ELSE 0 END) = 0 THEN 0
                     ELSE SUM(CASE WHEN pedido.status <> :statusCancelado THEN pedido.valorTotal ELSE 0 END)
                          / SUM(CASE WHEN pedido.status <> :statusCancelado THEN 1 ELSE 0 END) END
            )
            FROM Pedido pedido
            JOIN pedido.cliente cliente
            """;

    String AGRUPAMENTO_DIAS_RELATORIO_FINANCEIRO = """
            GROUP BY pedido.dataAgendada
            """;

    @Query(
            value = SELECT_DIAS_RELATORIO_FINANCEIRO + FILTROS_RELATORIO_FINANCEIRO + AGRUPAMENTO_DIAS_RELATORIO_FINANCEIRO,
            countQuery = "SELECT COUNT(DISTINCT pedido.dataAgendada) FROM Pedido pedido JOIN pedido.cliente cliente "
                    + FILTROS_RELATORIO_FINANCEIRO
    )
    Page<RelatorioFinanceiroDiaResponse> buscarDiasRelatorioFinanceiro(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("status") StatusPedido status,
            @Param("cliente") String cliente,
            @Param("statusCancelado") StatusPedido statusCancelado,
            Pageable pageable
    );

    @Query(SELECT_DIAS_RELATORIO_FINANCEIRO + FILTROS_RELATORIO_FINANCEIRO + AGRUPAMENTO_DIAS_RELATORIO_FINANCEIRO)
    Slice<RelatorioFinanceiroDiaResponse> buscarLoteDiasRelatorioFinanceiro(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("status") StatusPedido status,
            @Param("cliente") String cliente,
            @Param("statusCancelado") StatusPedido statusCancelado,
            Pageable pageable
    );

    @Query("""
            SELECT new br.com.sergio.gestaopedidos.dto.relatorio.RelatorioFinanceiroPagamentoResponse(
                pedido.formaPagamento,
                COUNT(pedido),
                SUM(pedido.valorTotal),
                CASE WHEN SUM(SUM(pedido.valorTotal)) OVER () = 0 THEN 0
                     ELSE SUM(pedido.valorTotal) * 100 / SUM(SUM(pedido.valorTotal)) OVER () END,
                SUM(pedido.valorTotal) / COUNT(pedido)
            )
            FROM Pedido pedido
            JOIN pedido.cliente cliente
            """ + FILTROS_RELATORIO_FINANCEIRO + """
            AND pedido.status <> :statusCancelado
            GROUP BY pedido.formaPagamento
            ORDER BY SUM(pedido.valorTotal) DESC, pedido.formaPagamento ASC
            """)
    List<RelatorioFinanceiroPagamentoResponse> buscarPagamentosRelatorioFinanceiro(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("status") StatusPedido status,
            @Param("cliente") String cliente,
            @Param("statusCancelado") StatusPedido statusCancelado
    );

    @Query("""
            SELECT new br.com.sergio.gestaopedidos.dto.relatorio.RelatorioFinanceiroEntregaResponse(
                pedido.tipoEntrega,
                COUNT(pedido),
                SUM(pedido.subtotal - COALESCE(pedido.valorDescontoGeral, 0)),
                SUM(pedido.taxaEntrega),
                SUM(pedido.valorTotal),
                CASE WHEN SUM(SUM(pedido.valorTotal)) OVER () = 0 THEN 0
                     ELSE SUM(pedido.valorTotal) * 100 / SUM(SUM(pedido.valorTotal)) OVER () END
            )
            FROM Pedido pedido
            JOIN pedido.cliente cliente
            """ + FILTROS_RELATORIO_FINANCEIRO + """
            AND pedido.status <> :statusCancelado
            GROUP BY pedido.tipoEntrega
            ORDER BY SUM(pedido.valorTotal) DESC, pedido.tipoEntrega ASC
            """)
    List<RelatorioFinanceiroEntregaResponse> buscarEntregasRelatorioFinanceiro(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("status") StatusPedido status,
            @Param("cliente") String cliente,
            @Param("statusCancelado") StatusPedido statusCancelado
    );

    @Query("""
            SELECT pedido.dataAgendada AS data, SUM(pedido.valorTotal) AS faturamento
            FROM Pedido pedido
            JOIN pedido.cliente cliente
            """ + FILTROS_RELATORIO_FINANCEIRO + """
            AND pedido.status <> :statusCancelado
            GROUP BY pedido.dataAgendada
            ORDER BY SUM(pedido.valorTotal) DESC, pedido.dataAgendada ASC
            """)
    List<MelhorDiaRelatorioFinanceiro> buscarMelhorDiaRelatorioFinanceiro(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("status") StatusPedido status,
            @Param("cliente") String cliente,
            @Param("statusCancelado") StatusPedido statusCancelado,
            Pageable pageable
    );

    String FILTROS_RELATORIO_CLIENTES = """
            WHERE pedido.dataAgendada BETWEEN :dataInicial AND :dataFinal
            AND pedido.status <> :statusCancelado
            AND (
                :cliente = ''
                OR LOWER(cliente.nome) LIKE LOWER(CONCAT('%', :cliente, '%'))
                OR cliente.telefone LIKE CONCAT('%', :cliente, '%')
            )
            AND (:tipoEntrega IS NULL OR pedido.tipoEntrega = :tipoEntrega)
            AND (:formaPagamento IS NULL OR pedido.formaPagamento = :formaPagamento)
            """;

    String AGRUPAMENTO_RELATORIO_CLIENTES = """
            GROUP BY cliente.id, cliente.nome, cliente.telefone
            HAVING (:minimoPedidos IS NULL OR COUNT(DISTINCT pedido.id) >= :minimoPedidos)
            AND (:minimoMovimentado IS NULL OR SUM(pedido.valorTotal) >= :minimoMovimentado)
            """;

    String SELECT_RELATORIO_CLIENTES = """
            SELECT new br.com.sergio.gestaopedidos.dto.relatorio.RelatorioClienteLinhaResponse(
                cliente.id,
                cliente.nome,
                cliente.telefone,
                COUNT(DISTINCT pedido.id),
                SUM(pedido.valorTotal),
                SUM(pedido.valorTotal) / COUNT(DISTINCT pedido.id),
                MIN(pedido.dataAgendada),
                MAX(pedido.dataAgendada),
                SUM(CASE WHEN pedido.tipoEntrega = :entrega THEN 1 ELSE 0 END),
                SUM(CASE WHEN pedido.tipoEntrega = :retirada THEN 1 ELSE 0 END),
                CASE WHEN SUM(SUM(pedido.valorTotal)) OVER () = 0 THEN 0
                     ELSE SUM(pedido.valorTotal) * 100 / SUM(SUM(pedido.valorTotal)) OVER () END,
                ROW_NUMBER() OVER (
                    ORDER BY SUM(pedido.valorTotal) DESC,
                             COUNT(DISTINCT pedido.id) DESC,
                             cliente.nome ASC
                )
            )
            FROM Pedido pedido
            JOIN pedido.cliente cliente
            """;

    @Query(SELECT_RELATORIO_CLIENTES + FILTROS_RELATORIO_CLIENTES + AGRUPAMENTO_RELATORIO_CLIENTES)
    Slice<RelatorioClienteLinhaResponse> buscarRelatorioClientes(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("cliente") String cliente,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("minimoPedidos") Long minimoPedidos,
            @Param("minimoMovimentado") BigDecimal minimoMovimentado,
            @Param("statusCancelado") StatusPedido statusCancelado,
            @Param("entrega") TipoEntrega entrega,
            @Param("retirada") TipoEntrega retirada,
            Pageable pageable
    );

    @Query(SELECT_RELATORIO_CLIENTES + FILTROS_RELATORIO_CLIENTES + AGRUPAMENTO_RELATORIO_CLIENTES)
    Slice<RelatorioClienteLinhaResponse> buscarLoteRelatorioClientes(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("cliente") String cliente,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("minimoPedidos") Long minimoPedidos,
            @Param("minimoMovimentado") BigDecimal minimoMovimentado,
            @Param("statusCancelado") StatusPedido statusCancelado,
            @Param("entrega") TipoEntrega entrega,
            @Param("retirada") TipoEntrega retirada,
            Pageable pageable
    );

    @Query(value = """
            SELECT
                COUNT(*) AS clientesCompradores,
                COALESCE(SUM(agrupado.pedidos_validos), 0) AS pedidosValidos,
                COALESCE(SUM(agrupado.faturamento_total), 0) AS faturamentoTotal,
                COALESCE(SUM(CASE WHEN agrupado.pedidos_validos >= 2 THEN 1 ELSE 0 END), 0) AS clientesRecorrentes
            FROM (
                SELECT p.cliente_id,
                       COUNT(DISTINCT p.id) AS pedidos_validos,
                       SUM(p.valor_total) AS faturamento_total
                FROM pedidos p
                JOIN clientes c ON c.id = p.cliente_id
                WHERE p.data_agendada BETWEEN :dataInicial AND :dataFinal
                AND p.status <> :statusCancelado
                AND (
                    :cliente = ''
                    OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :cliente, '%'))
                    OR c.telefone LIKE CONCAT('%', :cliente, '%')
                )
                AND (:tipoEntrega IS NULL OR p.tipo_entrega = :tipoEntrega)
                AND (:formaPagamento IS NULL OR p.forma_pagamento = :formaPagamento)
                GROUP BY p.cliente_id
                HAVING (:minimoPedidos IS NULL OR COUNT(DISTINCT p.id) >= :minimoPedidos)
                AND (:minimoMovimentado IS NULL OR SUM(p.valor_total) >= :minimoMovimentado)
            ) agrupado
            """, nativeQuery = true)
    IndicadoresRelatorioClientes buscarIndicadoresRelatorioClientes(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("cliente") String cliente,
            @Param("tipoEntrega") String tipoEntrega,
            @Param("formaPagamento") String formaPagamento,
            @Param("minimoPedidos") Long minimoPedidos,
            @Param("minimoMovimentado") BigDecimal minimoMovimentado,
            @Param("statusCancelado") String statusCancelado
    );

    @Query("""
            SELECT cliente.nome AS clienteNome, SUM(pedido.valorTotal) AS faturamentoTotal
            FROM Pedido pedido
            JOIN pedido.cliente cliente
            """ + FILTROS_RELATORIO_CLIENTES + AGRUPAMENTO_RELATORIO_CLIENTES + """
            ORDER BY SUM(pedido.valorTotal) DESC,
                     COUNT(DISTINCT pedido.id) DESC,
                     cliente.nome ASC
            """)
    List<ClienteLiderRelatorio> buscarClienteLiderRelatorio(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("cliente") String cliente,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("minimoPedidos") Long minimoPedidos,
            @Param("minimoMovimentado") BigDecimal minimoMovimentado,
            @Param("statusCancelado") StatusPedido statusCancelado,
            Pageable pageable
    );

    boolean existsByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "itens", "itens.produto"})
    List<Pedido> findByDataAgendadaOrderByDataPedidoAsc(LocalDate dataAgendada);

    @EntityGraph(attributePaths = {"cliente"})
    @Query("""
            SELECT p
            FROM Pedido p
            JOIN p.cliente c
            WHERE (
                :filtro = ''
                OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :filtro, '%'))
                OR c.telefone LIKE CONCAT('%', :filtro, '%')
            )
            AND (:status IS NULL OR p.status = :status)
            AND (:dataAgendada IS NULL OR p.dataAgendada = :dataAgendada)
            AND (:tipoEntrega IS NULL OR p.tipoEntrega = :tipoEntrega)
            AND (:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento)
            AND (:situacaoEstoque = ''
                OR (:situacaoEstoque = 'AGUARDANDO' AND p.estoqueMovimentado = false AND p.status <> br.com.sergio.gestaopedidos.enums.StatusPedido.CANCELADO)
                OR (:situacaoEstoque = 'BAIXADO' AND p.estoqueMovimentado = true)
                OR (:situacaoEstoque = 'ESTORNADO' AND p.status = br.com.sergio.gestaopedidos.enums.StatusPedido.CANCELADO AND EXISTS (SELECT m.id FROM MovimentacaoEstoque m WHERE m.pedido = p AND m.tipo = br.com.sergio.gestaopedidos.enums.TipoMovimentacaoEstoque.ESTORNO_SAIDA))
                OR (:situacaoEstoque = 'NAO_APLICA' AND p.status = br.com.sergio.gestaopedidos.enums.StatusPedido.CANCELADO AND NOT EXISTS (SELECT m.id FROM MovimentacaoEstoque m WHERE m.pedido = p AND m.tipo = br.com.sergio.gestaopedidos.enums.TipoMovimentacaoEstoque.SAIDA_VENDA)))
            """)
    Page<Pedido> buscarPaginado(
            @Param("filtro") String filtro,
            @Param("status") StatusPedido status,
            @Param("dataAgendada") LocalDate dataAgendada,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("situacaoEstoque") String situacaoEstoque,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"cliente"})
    @Query("""
            SELECT p FROM Pedido p
            WHERE p.dataAgendada = :data
              AND p.tipoEntrega = br.com.sergio.gestaopedidos.enums.TipoEntrega.ENTREGA
              AND p.status IN :status
            ORDER BY CASE WHEN p.horarioInicio IS NULL THEN 1 ELSE 0 END,
                     p.horarioInicio ASC, p.id ASC
            """)
    List<Pedido> buscarParaPlanejamento(
            @Param("data") LocalDate data,
            @Param("status") Collection<StatusPedido> status
    );

    @Query("""
            SELECT new br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoLinhaResponse(
                p.id,
                p.dataAgendada,
                p.dataPedido,
                c.nome,
                c.telefone,
                p.status,
                p.tipoEntrega,
                p.formaPagamento,
                p.subtotal,
                p.taxaEntrega,
                p.valorTotal
            )
            FROM Pedido p
            JOIN p.cliente c
            WHERE p.dataAgendada BETWEEN :dataInicial AND :dataFinal
            AND (
                :cliente = ''
                OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :cliente, '%'))
                OR c.telefone LIKE CONCAT('%', :cliente, '%')
            )
            AND (:status IS NULL OR p.status = :status)
            AND (:tipoEntrega IS NULL OR p.tipoEntrega = :tipoEntrega)
            AND (:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento)
            """)
    Page<RelatorioPedidoLinhaResponse> buscarRelatorioPedidos(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("cliente") String cliente,
            @Param("status") StatusPedido status,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            Pageable pageable
    );

    @Query("""
            SELECT new br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoLinhaResponse(
                p.id,
                p.dataAgendada,
                p.dataPedido,
                c.nome,
                c.telefone,
                p.status,
                p.tipoEntrega,
                p.formaPagamento,
                p.subtotal,
                p.taxaEntrega,
                p.valorTotal
            )
            FROM Pedido p
            JOIN p.cliente c
            WHERE p.dataAgendada BETWEEN :dataInicial AND :dataFinal
            AND (
                :cliente = ''
                OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :cliente, '%'))
                OR c.telefone LIKE CONCAT('%', :cliente, '%')
            )
            AND (:status IS NULL OR p.status = :status)
            AND (:tipoEntrega IS NULL OR p.tipoEntrega = :tipoEntrega)
            AND (:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento)
            """)
    Slice<RelatorioPedidoLinhaResponse> buscarLoteRelatorioPedidos(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("cliente") String cliente,
            @Param("status") StatusPedido status,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            Pageable pageable
    );

    @Query("""
            SELECT
                COUNT(p) AS totalPedidos,
                COALESCE(SUM(CASE WHEN p.status <> :statusCancelado THEN 1 ELSE 0 END), 0) AS pedidosValidos,
                COALESCE(SUM(CASE WHEN p.status = :statusCancelado THEN 1 ELSE 0 END), 0) AS cancelados,
                COALESCE(SUM(CASE WHEN p.status <> :statusCancelado THEN p.valorTotal ELSE 0 END), 0) AS faturamento,
                COALESCE(SUM(CASE WHEN p.status <> :statusCancelado THEN p.taxaEntrega ELSE 0 END), 0) AS taxasEntrega
            FROM Pedido p
            JOIN p.cliente c
            WHERE p.dataAgendada BETWEEN :dataInicial AND :dataFinal
            AND (
                :cliente = ''
                OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :cliente, '%'))
                OR c.telefone LIKE CONCAT('%', :cliente, '%')
            )
            AND (:status IS NULL OR p.status = :status)
            AND (:tipoEntrega IS NULL OR p.tipoEntrega = :tipoEntrega)
            AND (:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento)
            """)
    IndicadoresRelatorioPedidos buscarIndicadoresRelatorioPedidos(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("cliente") String cliente,
            @Param("status") StatusPedido status,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("statusCancelado") StatusPedido statusCancelado
    );

    @Query("""
            SELECT COALESCE(SUM(p.taxaEntrega), 0)
            FROM Pedido p
            WHERE p.dataAgendada BETWEEN :dataInicial AND :dataFinal
            AND p.status <> :statusCancelado
            AND (:tipoEntrega IS NULL OR p.tipoEntrega = :tipoEntrega)
            AND (:formaPagamento IS NULL OR p.formaPagamento = :formaPagamento)
            AND EXISTS (
                SELECT item.id FROM ItemPedido item
                JOIN item.produto produto
                WHERE item.pedido = p
                AND (:produto = '' OR LOWER(produto.nome) LIKE LOWER(CONCAT('%', :produto, '%')))
                AND (:unidadeVenda IS NULL OR produto.unidadeVenda = :unidadeVenda)
            )
            """)
    BigDecimal somarTaxasRelatorioProducao(
            @Param("dataInicial") LocalDate dataInicial,
            @Param("dataFinal") LocalDate dataFinal,
            @Param("produto") String produto,
            @Param("unidadeVenda") UnidadeVenda unidadeVenda,
            @Param("tipoEntrega") TipoEntrega tipoEntrega,
            @Param("formaPagamento") FormaPagamento formaPagamento,
            @Param("statusCancelado") StatusPedido statusCancelado
    );

    long countByDataAgendada(LocalDate dataAgendada);

    long countByDataAgendadaAndStatus(
            LocalDate dataAgendada,
            StatusPedido status
    );

    @Query("""
            SELECT COUNT(p) AS total,
                   SUM(CASE WHEN p.status <> :cancelado THEN 1 ELSE 0 END) AS validos,
                   SUM(CASE WHEN p.status = :cancelado THEN 1 ELSE 0 END) AS cancelados,
                   SUM(CASE WHEN p.status = :preparacao THEN 1 ELSE 0 END) AS emPreparacao,
                   SUM(CASE WHEN p.status = :saiuEntrega THEN 1 ELSE 0 END) AS saiuParaEntrega,
                   COALESCE(SUM(CASE WHEN p.status <> :cancelado THEN p.subtotal - COALESCE(p.valorDescontoGeral, 0) ELSE 0 END), 0) AS produtos,
                   COALESCE(SUM(CASE WHEN p.status <> :cancelado THEN p.taxaEntrega ELSE 0 END), 0) AS taxas,
                   COALESCE(SUM(CASE WHEN p.status <> :cancelado THEN p.valorTotal ELSE 0 END), 0) AS faturamento
            FROM Pedido p WHERE p.dataAgendada = :data
            """)
    ResumoDashboard resumirDashboard(@Param("data") LocalDate data,
                                      @Param("cancelado") StatusPedido cancelado,
                                      @Param("preparacao") StatusPedido preparacao,
                                      @Param("saiuEntrega") StatusPedido saiuEntrega);

    @Query("""
            SELECT COUNT(p) AS pedidosValidos,
                   COALESCE(SUM(CASE WHEN p.custoTotalHistorico IS NOT NULL THEN 1 ELSE 0 END), 0) AS pedidosComCusto,
                   COALESCE(SUM(CASE WHEN p.custoTotalHistorico IS NOT NULL THEN p.subtotal - COALESCE(p.valorDescontoGeral, 0) ELSE 0 END), 0) AS receitaProdutos,
                   COALESCE(SUM(p.custoTotalHistorico), 0) AS cmv,
                   COALESCE(SUM(p.lucroBrutoEstimado), 0) AS lucroBruto
            FROM Pedido p
            WHERE p.dataAgendada = :data
              AND p.status <> :cancelado
            """)
    ResumoFinanceiroDashboard resumirFinanceiroDashboard(@Param("data") LocalDate data,
                                                          @Param("cancelado") StatusPedido cancelado);

    @Query("""
            SELECT COALESCE(SUM(p.valorTotal), 0)
            FROM Pedido p
            WHERE p.dataAgendada = :dataAgendada
            AND p.status <> :statusExcluido
            """)
    BigDecimal somarValorTotalPorDataExcetoStatus(
            @Param("dataAgendada") LocalDate dataAgendada,
            @Param("statusExcluido") StatusPedido statusExcluido
    );

    @Query("""
            SELECT COALESCE(SUM(p.taxaEntrega), 0)
            FROM Pedido p
            WHERE p.dataAgendada = :dataAgendada
            AND p.status <> :statusExcluido
            """)
    BigDecimal somarTaxasEntregaPorDataExcetoStatus(
            @Param("dataAgendada") LocalDate dataAgendada,
            @Param("statusExcluido") StatusPedido statusExcluido
    );

    @Query("""
            SELECT new br.com.sergio.gestaopedidos.dto.dashboard.DashboardPedidoAtencaoResponse(
                p.id,
                c.nome,
                p.tipoEntrega,
                p.status,
                p.valorTotal,
                p.estoqueMovimentado
            )
            FROM Pedido p
            JOIN p.cliente c
            WHERE p.dataAgendada = :dataAgendada
            AND p.status IN :statusAtencao
            ORDER BY CASE
                WHEN p.status = :statusSaiuEntrega THEN 1
                WHEN p.status = :statusPronto THEN 2
                WHEN p.status = :statusEmPreparacao THEN 3
                WHEN p.status = :statusPendente THEN 4
                ELSE 5
            END,
            p.dataPedido ASC,
            p.id ASC
            """)
    List<DashboardPedidoAtencaoResponse> buscarPedidosQuePrecisamAtencao(
            @Param("dataAgendada") LocalDate dataAgendada,
            @Param("statusAtencao") Set<StatusPedido> statusAtencao,
            @Param("statusPronto") StatusPedido statusPronto,
            @Param("statusSaiuEntrega") StatusPedido statusSaiuEntrega,
            @Param("statusEmPreparacao") StatusPedido statusEmPreparacao,
            @Param("statusPendente") StatusPedido statusPendente,
            Pageable pageable
    );

    @Query("""
            SELECT p.status AS status, COUNT(p) AS quantidade
            FROM Pedido p
            WHERE p.dataAgendada = :dataAgendada
            GROUP BY p.status
            """)
    List<ContagemPedidosPorStatus> contarPedidosPorStatusNaData(
            @Param("dataAgendada") LocalDate dataAgendada
    );
}
