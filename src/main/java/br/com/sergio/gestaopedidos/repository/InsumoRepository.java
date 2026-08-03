package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Insumo;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    boolean existsByNomeIgnoreCase(String nome);
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    @Query("""
            SELECT i FROM Insumo i
            WHERE (:filtro = '' OR LOWER(i.nome) LIKE LOWER(CONCAT('%', :filtro, '%'))
                   OR LOWER(COALESCE(i.descricao, '')) LIKE LOWER(CONCAT('%', :filtro, '%')))
              AND (:ativo IS NULL OR i.ativo = :ativo)
            """)
    Page<Insumo> buscar(@Param("filtro") String filtro, @Param("ativo") Boolean ativo, Pageable pageable);
}
