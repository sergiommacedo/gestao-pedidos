package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Producao;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;

public interface ProducaoRepository extends JpaRepository<Producao, Long> {
    boolean existsByDataProducao(LocalDate dataProducao);
    boolean existsByDataProducaoAndIdNot(LocalDate dataProducao, Long id);
    Optional<Producao> findByDataProducao(LocalDate dataProducao);

    @Query("""
            SELECT p FROM Producao p
            WHERE (:dataInicial IS NULL OR p.dataProducao >= :dataInicial)
            AND (:dataFinal IS NULL OR p.dataProducao <= :dataFinal)
            """)
    Page<Producao> buscarPorPeriodo(@Param("dataInicial") LocalDate dataInicial,
                                    @Param("dataFinal") LocalDate dataFinal, Pageable pageable);
}
