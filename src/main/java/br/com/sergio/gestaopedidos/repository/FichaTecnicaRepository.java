package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.FichaTecnica;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface FichaTecnicaRepository extends JpaRepository<FichaTecnica, Long> {
    boolean existsByProdutoId(Long produtoId);
    Optional<FichaTecnica> findByProdutoId(Long produtoId);

    @EntityGraph(attributePaths = {"produto", "itens", "itens.insumo"})
    @Query("SELECT DISTINCT f FROM FichaTecnica f WHERE f.id = :id")
    Optional<FichaTecnica> buscarDetalhada(@Param("id") Long id);

    @EntityGraph(attributePaths = {"produto", "itens", "itens.insumo"})
    @Query("SELECT DISTINCT f FROM FichaTecnica f WHERE f.id IN :ids")
    List<FichaTecnica> buscarDetalhadas(@Param("ids") Collection<Long> ids);

    @Query(value = """
            SELECT f.* FROM fichas_tecnicas f
            JOIN produtos p ON p.id = f.produto_id
            WHERE (:produto = '' OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :produto, '%')))
              AND (:ativa IS NULL OR f.ativa = :ativa)
              AND (:custo = ''
                OR (:custo = 'PENDENTE' AND EXISTS (
                    SELECT 1 FROM itens_ficha_tecnica i
                    LEFT JOIN saldos_estoque s ON s.insumo_id = i.insumo_id
                    WHERE i.ficha_tecnica_id = f.id AND COALESCE(s.custo_medio_atual, 0) <= 0))
                OR (:custo = 'COMPLETO' AND NOT EXISTS (
                    SELECT 1 FROM itens_ficha_tecnica i
                    LEFT JOIN saldos_estoque s ON s.insumo_id = i.insumo_id
                    WHERE i.ficha_tecnica_id = f.id AND COALESCE(s.custo_medio_atual, 0) <= 0)))
            ORDER BY p.nome ASC
            """, countQuery = """
            SELECT COUNT(*) FROM fichas_tecnicas f
            JOIN produtos p ON p.id = f.produto_id
            WHERE (:produto = '' OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :produto, '%')))
              AND (:ativa IS NULL OR f.ativa = :ativa)
              AND (:custo = ''
                OR (:custo = 'PENDENTE' AND EXISTS (
                    SELECT 1 FROM itens_ficha_tecnica i LEFT JOIN saldos_estoque s ON s.insumo_id = i.insumo_id
                    WHERE i.ficha_tecnica_id = f.id AND COALESCE(s.custo_medio_atual, 0) <= 0))
                OR (:custo = 'COMPLETO' AND NOT EXISTS (
                    SELECT 1 FROM itens_ficha_tecnica i LEFT JOIN saldos_estoque s ON s.insumo_id = i.insumo_id
                    WHERE i.ficha_tecnica_id = f.id AND COALESCE(s.custo_medio_atual, 0) <= 0)))
            """, nativeQuery = true)
    Page<FichaTecnica> listar(@Param("produto") String produto, @Param("ativa") Boolean ativa,
                              @Param("custo") String custo, Pageable pageable);
}
