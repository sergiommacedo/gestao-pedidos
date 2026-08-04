package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Producao;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface ProducaoRepository extends JpaRepository<Producao, Long> {
    interface ResumoDashboard {Long getProducoes();Long getProdutos();java.math.BigDecimal getQuantidade();java.math.BigDecimal getCusto();}
    @Query("""
            SELECT CASE WHEN COUNT(i)>0 THEN true ELSE false END
            FROM Producao p JOIN p.itens i
            WHERE p.status=br.com.sergio.gestaopedidos.enums.StatusProducao.RASCUNHO
              AND i.produto.id IN :produtoIds
              AND (:ignorarId IS NULL OR p.id<>:ignorarId)
            """)
    boolean existeRascunhoComPreparacao(@Param("produtoIds") Collection<Long> produtoIds,
                                         @Param("ignorarId") Long ignorarId);
    @Query("""
            SELECT COUNT(DISTINCT p.id) AS producoes,COUNT(DISTINCT i.produto.id) AS produtos,
                   COALESCE(SUM(CASE WHEN i.unidadeHistorica=br.com.sergio.gestaopedidos.enums.UnidadeMedida.QUILOGRAMA THEN i.quantidade ELSE 0 END),0) AS quantidade,
                   COALESCE(SUM(i.custoTotal),0) AS custo
            FROM Producao p LEFT JOIN p.itens i
            WHERE p.dataProducao=:data
              AND p.status=br.com.sergio.gestaopedidos.enums.StatusProducao.CONFIRMADA
            """) ResumoDashboard resumirConfirmadasDashboard(@Param("data")LocalDate data);
    @EntityGraph(attributePaths={"itens","itens.produto"})
    @Query("SELECT DISTINCT p FROM Producao p WHERE p.dataProducao=:data AND p.status=br.com.sergio.gestaopedidos.enums.StatusProducao.RASCUNHO ORDER BY p.id")
    java.util.List<Producao> buscarRascunhosDashboard(@Param("data") LocalDate data);
    Optional<Producao> findFirstByOrderByDataProducaoDesc();
    Optional<Producao> findFirstByDataProducaoLessThanOrderByDataProducaoDesc(LocalDate dataProducao);
    @EntityGraph(attributePaths={"itens","itens.produto"}) @Query("SELECT p FROM Producao p WHERE p.id=:id") Optional<Producao> buscarDetalhada(@Param("id")Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @EntityGraph(attributePaths={"itens","itens.produto"}) @Query("SELECT p FROM Producao p WHERE p.id=:id") Optional<Producao> bloquearDetalhada(@Param("id")Long id);

    @Query("""
            SELECT p FROM Producao p
            WHERE (:dataInicial IS NULL OR p.dataProducao >= :dataInicial)
            AND (:dataFinal IS NULL OR p.dataProducao <= :dataFinal)
            """)
    Page<Producao> buscarPorPeriodo(@Param("dataInicial") LocalDate dataInicial,
                                    @Param("dataFinal") LocalDate dataFinal, Pageable pageable);
}
