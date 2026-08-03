package br.com.sergio.gestaopedidos.repository;
import br.com.sergio.gestaopedidos.entity.*;import jakarta.persistence.LockModeType;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.Optional;
public interface EstoqueInsumoRepository extends JpaRepository<EstoqueInsumo,Long>{
 interface Saldo{Long getInsumoId();String getInsumoNome();br.com.sergio.gestaopedidos.enums.UnidadeMedida getUnidadeMedida();Boolean getAtivo();BigDecimal getQuantidadeAtual();BigDecimal getEstoqueMinimo();BigDecimal getCustoMedioAtual();BigDecimal getValorTotalEstoque();LocalDateTime getAtualizadoEm();}
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("SELECT i FROM Insumo i WHERE i.id=:id")Optional<Insumo> bloquearInsumo(@Param("id")Long id);
 @Query("SELECT i FROM Insumo i WHERE i.id=:id")Optional<Insumo> buscarInsumo(@Param("id")Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE)Optional<EstoqueInsumo> findByInsumoId(Long id);
 @Query("SELECT e FROM EstoqueInsumo e WHERE e.insumo.id=:id")Optional<EstoqueInsumo> buscarSaldo(@Param("id")Long id);
 @Query("""
 SELECT i.id AS insumoId,i.nome AS insumoNome,i.unidadeMedida AS unidadeMedida,i.ativo AS ativo,
 COALESCE(e.quantidadeAtual,0) AS quantidadeAtual,i.estoqueMinimo AS estoqueMinimo,
 COALESCE(e.custoMedioAtual,0) AS custoMedioAtual,COALESCE(e.valorTotalEstoque,0) AS valorTotalEstoque,e.atualizadoEm AS atualizadoEm
 FROM Insumo i LEFT JOIN EstoqueInsumo e ON e.insumo=i
 WHERE (:nome='' OR LOWER(i.nome) LIKE LOWER(CONCAT('%',:nome,'%'))) AND (:ativo IS NULL OR i.ativo=:ativo)
 AND (:situacao='' OR (:situacao='SEM_ESTOQUE' AND COALESCE(e.quantidadeAtual,0)=0)
 OR (:situacao='ABAIXO' AND i.estoqueMinimo>0 AND COALESCE(e.quantidadeAtual,0)<=i.estoqueMinimo)
 OR (:situacao='NORMAL' AND COALESCE(e.quantidadeAtual,0)>0 AND (i.estoqueMinimo=0 OR e.quantidadeAtual>i.estoqueMinimo)))
 """)Page<Saldo> listar(@Param("nome")String nome,@Param("situacao")String situacao,@Param("ativo")Boolean ativo,Pageable pageable);
 long countByQuantidadeAtualGreaterThan(BigDecimal zero);
 @Query("SELECT COUNT(i) FROM Insumo i LEFT JOIN EstoqueInsumo e ON e.insumo=i WHERE i.estoqueMinimo>0 AND COALESCE(e.quantidadeAtual,0)<=i.estoqueMinimo")long contarAbaixoMinimo();
 @Query("SELECT COALESCE(SUM(e.valorTotalEstoque),0) FROM EstoqueInsumo e")BigDecimal somarValorEstoque();
}
