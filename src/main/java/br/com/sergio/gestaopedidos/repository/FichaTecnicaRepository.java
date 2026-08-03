package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.FichaTecnica;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface FichaTecnicaRepository extends JpaRepository<FichaTecnica, Long> {
    @Query(value="SELECT COUNT(*) FROM fichas_tecnicas f WHERE f.ativa=true AND EXISTS (SELECT 1 FROM itens_ficha_tecnica i LEFT JOIN saldos_estoque s ON s.insumo_id=i.insumo_id WHERE i.ficha_tecnica_id=f.id AND COALESCE(s.custo_medio_atual,0)<=0)",nativeQuery=true) long contarAtivasComCustoPendente();
    @Query(value="SELECT COUNT(*) FROM produtos p WHERE p.ativo=true AND p.tipo_produto='PRODUZIDO' AND NOT EXISTS (SELECT 1 FROM fichas_tecnicas f WHERE f.produto_id=p.id)",nativeQuery=true) long contarProdutosProduzidosAtivosSemFicha();
    boolean existsByProdutoId(Long produtoId);
    Optional<FichaTecnica> findByProdutoId(Long produtoId);

    @EntityGraph(attributePaths = {"produto", "itens", "itens.insumo"})
    @Query("SELECT DISTINCT f FROM FichaTecnica f WHERE f.id = :id")
    Optional<FichaTecnica> buscarDetalhada(@Param("id") Long id);

    @EntityGraph(attributePaths = {"produto", "itens", "itens.insumo"})
    @Query("SELECT DISTINCT f FROM FichaTecnica f WHERE f.id IN :ids")
    List<FichaTecnica> buscarDetalhadas(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"produto", "itens", "itens.insumo"})
    @Query("SELECT DISTINCT f FROM FichaTecnica f WHERE f.produto.id IN :produtoIds AND f.ativa = true")
    List<FichaTecnica> buscarAtivasPorProdutos(@Param("produtoIds") Collection<Long> produtoIds);

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
