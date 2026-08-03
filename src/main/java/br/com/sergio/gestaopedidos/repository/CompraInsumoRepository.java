package br.com.sergio.gestaopedidos.repository;
import br.com.sergio.gestaopedidos.entity.CompraInsumo;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Optional;
public interface CompraInsumoRepository extends JpaRepository<CompraInsumo,Long>{
    interface Resumo {Long getId();LocalDate getDataCompra();String getFornecedor();java.math.BigDecimal getValorTotal();java.time.LocalDateTime getAtualizadoEm();Long getQuantidadeItens();br.com.sergio.gestaopedidos.enums.StatusCompraInsumo getStatus();}
    @Query(value="""
            SELECT c.id AS id,c.dataCompra AS dataCompra,c.fornecedor AS fornecedor,c.valorTotal AS valorTotal,
                   c.atualizadoEm AS atualizadoEm,c.status AS status,COUNT(i.id) AS quantidadeItens
              FROM CompraInsumo c LEFT JOIN c.itens i
             WHERE (:inicio IS NULL OR c.dataCompra>=:inicio) AND (:fim IS NULL OR c.dataCompra<=:fim)
               AND (:fornecedor='' OR LOWER(COALESCE(c.fornecedor,'')) LIKE LOWER(CONCAT('%',:fornecedor,'%')))
             GROUP BY c.id,c.dataCompra,c.fornecedor,c.valorTotal,c.atualizadoEm,c.status
            """,countQuery="""
            SELECT COUNT(c) FROM CompraInsumo c
             WHERE (:inicio IS NULL OR c.dataCompra>=:inicio) AND (:fim IS NULL OR c.dataCompra<=:fim)
               AND (:fornecedor='' OR LOWER(COALESCE(c.fornecedor,'')) LIKE LOWER(CONCAT('%',:fornecedor,'%')))
            """)
    Page<Resumo> buscar(@Param("inicio")LocalDate inicio,@Param("fim")LocalDate fim,@Param("fornecedor")String fornecedor,Pageable pageable);
    @EntityGraph(attributePaths={"itens","itens.insumo"}) @Query("SELECT DISTINCT c FROM CompraInsumo c WHERE c.id=:id") Optional<CompraInsumo> buscarDetalhada(@Param("id")Long id);
}
