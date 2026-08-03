package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Producao;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface ProducaoRepository extends JpaRepository<Producao, Long> {
    boolean existsByDataProducao(LocalDate dataProducao);
    boolean existsByDataProducaoAndIdNot(LocalDate dataProducao, Long id);
    Optional<Producao> findByDataProducao(LocalDate dataProducao);
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
