package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Producao;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface ProducaoRepository extends JpaRepository<Producao, Long> {
    interface ResumoDashboard {Long getId();br.com.sergio.gestaopedidos.enums.StatusProducao getStatus();Long getProdutos();java.math.BigDecimal getQuantidade();java.math.BigDecimal getCusto();java.time.LocalDateTime getConfirmadaEm();}
    boolean existsByDataProducao(LocalDate dataProducao);
    boolean existsByDataProducaoAndIdNot(LocalDate dataProducao, Long id);
    Optional<Producao> findByDataProducao(LocalDate dataProducao);
    @Query("""
            SELECT p.id AS id,p.status AS status,COUNT(i.id) AS produtos,
                   COALESCE(SUM(i.quantidade),0) AS quantidade,
                   COALESCE(SUM(i.custoLote),0) AS custo,p.confirmadaEm AS confirmadaEm
            FROM Producao p LEFT JOIN p.itens i WHERE p.dataProducao=:data
            GROUP BY p.id,p.status,p.confirmadaEm
            """) Optional<ResumoDashboard> resumirDashboard(@Param("data")LocalDate data);
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
