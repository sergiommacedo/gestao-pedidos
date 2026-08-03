package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Compra;
import br.com.sergio.gestaopedidos.enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra,Long> {
    interface Resumo {Long getId();TipoCompra getTipoCompra();LocalDate getDataCompra();String getFornecedor();java.math.BigDecimal getValorTotal();LocalDateTime getAtualizadoEm();Long getQuantidadeItens();StatusCompra getStatus();}
    @Query(value="""
        SELECT c.id AS id,c.tipoCompra AS tipoCompra,c.dataCompra AS dataCompra,c.fornecedor AS fornecedor,
               c.valorTotal AS valorTotal,c.atualizadoEm AS atualizadoEm,c.status AS status,COUNT(i.id) AS quantidadeItens
          FROM Compra c LEFT JOIN c.itens i
         WHERE (:inicio IS NULL OR c.dataCompra>=:inicio) AND (:fim IS NULL OR c.dataCompra<=:fim)
           AND (:fornecedor='' OR LOWER(COALESCE(c.fornecedor,'')) LIKE LOWER(CONCAT('%',:fornecedor,'%')))
           AND (:tipo IS NULL OR c.tipoCompra=:tipo)
         GROUP BY c.id,c.tipoCompra,c.dataCompra,c.fornecedor,c.valorTotal,c.atualizadoEm,c.status
        """,countQuery="""
        SELECT COUNT(c) FROM Compra c WHERE (:inicio IS NULL OR c.dataCompra>=:inicio) AND (:fim IS NULL OR c.dataCompra<=:fim)
          AND (:fornecedor='' OR LOWER(COALESCE(c.fornecedor,'')) LIKE LOWER(CONCAT('%',:fornecedor,'%')))
          AND (:tipo IS NULL OR c.tipoCompra=:tipo)
        """)
    Page<Resumo> buscar(@Param("inicio")LocalDate inicio,@Param("fim")LocalDate fim,@Param("fornecedor")String fornecedor,@Param("tipo")TipoCompra tipo,Pageable pageable);
    @EntityGraph(attributePaths={"itens","itens.insumo","itens.produto"}) @Query("SELECT DISTINCT c FROM Compra c WHERE c.id=:id") Optional<Compra> buscarDetalhada(@Param("id")Long id);
}
