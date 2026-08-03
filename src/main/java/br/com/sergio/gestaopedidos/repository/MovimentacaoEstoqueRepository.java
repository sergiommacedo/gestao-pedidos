package br.com.sergio.gestaopedidos.repository;
import br.com.sergio.gestaopedidos.entity.MovimentacaoEstoque;import br.com.sergio.gestaopedidos.enums.TipoMovimentacaoEstoque;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.time.LocalDateTime;import java.util.*;
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque,Long>{
 boolean existsByItemCompraInsumoIdAndTipo(Long itemId,TipoMovimentacaoEstoque tipo);
 @EntityGraph(attributePaths={"insumo","itemCompraInsumo","itemCompraInsumo.compra"})@Query("""
 SELECT m FROM MovimentacaoEstoque m WHERE (:inicio IS NULL OR m.dataMovimentacao>=:inicio)
 AND (:fim IS NULL OR m.dataMovimentacao<=:fim) AND (:insumoId IS NULL OR m.insumo.id=:insumoId)
 AND (:tipo IS NULL OR m.tipo=:tipo) AND (:compraId IS NULL OR m.itemCompraInsumo.compra.id=:compraId)
 """)Page<MovimentacaoEstoque> listar(@Param("inicio")LocalDateTime inicio,@Param("fim")LocalDateTime fim,@Param("insumoId")Long insumoId,@Param("tipo")TipoMovimentacaoEstoque tipo,@Param("compraId")Long compraId,Pageable pageable);
 @EntityGraph(attributePaths={"insumo","itemCompraInsumo","itemCompraInsumo.compra"})List<MovimentacaoEstoque> findTop20ByInsumoIdOrderByDataMovimentacaoDescIdDesc(Long insumoId);
 @EntityGraph(attributePaths={"insumo","itemCompraInsumo"})List<MovimentacaoEstoque> findByItemCompraInsumoCompraIdAndTipoOrderByIdAsc(Long compraId,TipoMovimentacaoEstoque tipo);
 long countByDataMovimentacaoBetween(LocalDateTime inicio,LocalDateTime fim);
}
