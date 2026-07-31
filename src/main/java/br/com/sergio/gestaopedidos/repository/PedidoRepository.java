package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Pedido;
import br.com.sergio.gestaopedidos.dto.dashboard.DashboardPedidoAtencaoResponse;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    interface ContagemPedidosPorStatus {

        StatusPedido getStatus();

        long getQuantidade();
    }

    boolean existsByClienteId(Long clienteId);

    @EntityGraph(attributePaths = {"cliente", "itens", "itens.produto"})
    List<Pedido> findByDataAgendadaOrderByDataPedidoAsc(LocalDate dataAgendada);

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
            """)
    Page<Pedido> buscarPaginado(
            @Param("filtro") String filtro,
            @Param("status") StatusPedido status,
            @Param("dataAgendada") LocalDate dataAgendada,
            Pageable pageable
    );

    long countByDataAgendada(LocalDate dataAgendada);

    long countByDataAgendadaAndStatus(
            LocalDate dataAgendada,
            StatusPedido status
    );

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
            SELECT new br.com.sergio.gestaopedidos.dto.dashboard.DashboardPedidoAtencaoResponse(
                p.id,
                c.nome,
                p.tipoEntrega,
                p.status,
                p.valorTotal
            )
            FROM Pedido p
            JOIN p.cliente c
            WHERE p.dataAgendada = :dataAgendada
            AND p.status IN :statusAtencao
            ORDER BY CASE
                WHEN p.status = :statusPronto THEN 1
                WHEN p.status = :statusSaiuEntrega THEN 2
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
