package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Compra;
import br.com.sergio.gestaopedidos.enums.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra,Long> {
    interface ResumoOperacional {Long getComprasHoje();java.math.BigDecimal getTotalHoje();java.math.BigDecimal getTotalMes();java.math.BigDecimal getTotalPeriodo();}
    interface AnaliseFornecedor {String getTipoItem();Long getReferenciaId();String getItemNome();String getUnidade();String getFornecedor();LocalDate getUltimaCompra();java.math.BigDecimal getUltimoPreco();java.math.BigDecimal getMenorPreco();java.math.BigDecimal getMaiorPreco();java.math.BigDecimal getValorPago();java.math.BigDecimal getQuantidadeTotal();}
    interface HistoricoPreco {LocalDate getDataCompra();String getFornecedor();java.math.BigDecimal getQuantidade();java.math.BigDecimal getValorPago();java.math.BigDecimal getPrecoUnitario();}
    interface OpcaoItemAnalise {String getTipoItem();Long getReferenciaId();String getItemNome();String getUnidade();}
    interface ResumoDashboard {Long getQuantidade();java.math.BigDecimal getValorTotal();Long getComprasInsumos();java.math.BigDecimal getValorInsumos();Long getComprasRevenda();java.math.BigDecimal getValorRevenda();}
    interface CompraRecente {Long getId();TipoCompra getTipoCompra();LocalDate getDataCompra();String getFornecedor();java.math.BigDecimal getValorTotal();Long getQuantidadeItens();}
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
    @Query("""
        SELECT SUM(CASE WHEN c.dataCompra=:hoje THEN 1 ELSE 0 END) AS comprasHoje,
               COALESCE(SUM(CASE WHEN c.dataCompra=:hoje THEN c.valorTotal ELSE 0 END),0) AS totalHoje,
               COALESCE(SUM(CASE WHEN c.dataCompra>=:inicioMes AND c.dataCompra<=:fimMes THEN c.valorTotal ELSE 0 END),0) AS totalMes,
               COALESCE(SUM(CASE WHEN (:inicio IS NULL OR c.dataCompra>=:inicio) AND (:fim IS NULL OR c.dataCompra<=:fim) THEN c.valorTotal ELSE 0 END),0) AS totalPeriodo
          FROM Compra c
         WHERE c.status=br.com.sergio.gestaopedidos.enums.StatusCompra.ATIVA
        """)
    ResumoOperacional resumirOperacao(@Param("hoje")LocalDate hoje,@Param("inicioMes")LocalDate inicioMes,@Param("fimMes")LocalDate fimMes,
                                      @Param("inicio")LocalDate inicio,@Param("fim")LocalDate fim);
    @Query(value="""
        WITH base AS (
          SELECT CASE WHEN ic.insumo_id IS NOT NULL THEN 'INSUMO' ELSE 'PRODUTO_REVENDA' END tipo_item,
                 COALESCE(ic.insumo_id,ic.produto_id) referencia_id,ic.nome_historico item_nome,
                 ic.unidade_historica unidade,COALESCE(NULLIF(TRIM(c.fornecedor),''),'Não informado') fornecedor,
                 c.data_compra,ic.custo_unitario,ic.valor_total_item,ic.quantidade,ic.id item_id
            FROM itens_compra ic JOIN compras c ON c.id=ic.compra_id
           WHERE c.status='ATIVA'
             AND (:tipoItem='' OR (CASE WHEN ic.insumo_id IS NOT NULL THEN 'INSUMO' ELSE 'PRODUTO_REVENDA' END)=:tipoItem)
             AND (:referenciaId IS NULL OR COALESCE(ic.insumo_id,ic.produto_id)=:referenciaId)
             AND (:unidade='' OR ic.unidade_historica=:unidade)
             AND (:fornecedor='' OR LOWER(COALESCE(c.fornecedor,'')) LIKE LOWER(CONCAT('%',:fornecedor,'%')))
             AND (:categoria='' OR CASE WHEN ic.insumo_id IS NOT NULL THEN 'INSUMO' ELSE 'PRODUTO_REVENDA' END=:categoria)
             AND (:inicio IS NULL OR c.data_compra>=:inicio) AND (:fim IS NULL OR c.data_compra<=:fim)
        ), ordenada AS (
          SELECT base.*,ROW_NUMBER() OVER(PARTITION BY tipo_item,referencia_id,unidade,fornecedor ORDER BY data_compra DESC,item_id DESC) posicao
            FROM base
        )
        SELECT tipo_item,referencia_id,MAX(item_nome) item_nome,unidade,fornecedor,MAX(data_compra) ultima_compra,
               MAX(CASE WHEN posicao=1 THEN custo_unitario END) ultimo_preco,
               MIN(custo_unitario) menor_preco,MAX(custo_unitario) maior_preco,
               SUM(valor_total_item) valor_pago,SUM(quantidade) quantidade_total
          FROM ordenada GROUP BY tipo_item,referencia_id,unidade,fornecedor
         ORDER BY item_nome,unidade,fornecedor
        """,nativeQuery=true)
    java.util.List<AnaliseFornecedor> analisarPrecos(@Param("tipoItem")String tipoItem,@Param("referenciaId")Long referenciaId,
            @Param("unidade")String unidade,@Param("fornecedor")String fornecedor,@Param("categoria")String categoria,
            @Param("inicio")LocalDate inicio,@Param("fim")LocalDate fim);
    @Query(value="""
        SELECT c.data_compra data_compra,COALESCE(NULLIF(TRIM(c.fornecedor),''),'Não informado') fornecedor,
               ic.quantidade,ic.valor_total_item valor_pago,ic.custo_unitario preco_unitario
          FROM itens_compra ic JOIN compras c ON c.id=ic.compra_id
         WHERE c.status='ATIVA' AND COALESCE(ic.insumo_id,ic.produto_id)=:referenciaId
           AND (CASE WHEN ic.insumo_id IS NOT NULL THEN 'INSUMO' ELSE 'PRODUTO_REVENDA' END)=:categoria
           AND ic.unidade_historica=:unidade
           AND (:fornecedor='' OR LOWER(COALESCE(c.fornecedor,'')) LIKE LOWER(CONCAT('%',:fornecedor,'%')))
           AND (:inicio IS NULL OR c.data_compra>=:inicio) AND (:fim IS NULL OR c.data_compra<=:fim)
         ORDER BY c.data_compra DESC,ic.id DESC
        """,countQuery="""
        SELECT COUNT(*) FROM itens_compra ic JOIN compras c ON c.id=ic.compra_id
         WHERE c.status='ATIVA' AND COALESCE(ic.insumo_id,ic.produto_id)=:referenciaId
           AND (CASE WHEN ic.insumo_id IS NOT NULL THEN 'INSUMO' ELSE 'PRODUTO_REVENDA' END)=:categoria
           AND ic.unidade_historica=:unidade
           AND (:fornecedor='' OR LOWER(COALESCE(c.fornecedor,'')) LIKE LOWER(CONCAT('%',:fornecedor,'%')))
           AND (:inicio IS NULL OR c.data_compra>=:inicio) AND (:fim IS NULL OR c.data_compra<=:fim)
        """,nativeQuery=true)
    Page<HistoricoPreco> buscarHistoricoPrecos(@Param("categoria")String categoria,
            @Param("referenciaId")Long referenciaId,@Param("unidade")String unidade,
            @Param("fornecedor")String fornecedor,@Param("inicio")LocalDate inicio,@Param("fim")LocalDate fim,Pageable pageable);
    @Query(value="""
        SELECT CASE WHEN ic.insumo_id IS NOT NULL THEN 'INSUMO' ELSE 'PRODUTO_REVENDA' END tipo_item,
               COALESCE(ic.insumo_id,ic.produto_id) referencia_id,MAX(ic.nome_historico) item_nome,ic.unidade_historica unidade
          FROM itens_compra ic JOIN compras c ON c.id=ic.compra_id
         WHERE c.status='ATIVA' AND (:termo='' OR LOWER(ic.nome_historico) LIKE LOWER(CONCAT('%',:termo,'%')))
         GROUP BY CASE WHEN ic.insumo_id IS NOT NULL THEN 'INSUMO' ELSE 'PRODUTO_REVENDA' END,
                  COALESCE(ic.insumo_id,ic.produto_id),ic.unidade_historica
         ORDER BY item_nome,tipo_item,unidade
        """,nativeQuery=true)
    java.util.List<OpcaoItemAnalise> buscarOpcoesItensAnalise(@Param("termo")String termo,Pageable pageable);
    @Query(value="""
        SELECT DISTINCT :tipoItem tipo_item,COALESCE(ic.insumo_id,ic.produto_id) referencia_id,
               ic.nome_historico item_nome,ic.unidade_historica unidade
          FROM itens_compra ic JOIN compras c ON c.id=ic.compra_id
         WHERE c.status='ATIVA' AND COALESCE(ic.insumo_id,ic.produto_id)=:referenciaId
           AND (CASE WHEN ic.insumo_id IS NOT NULL THEN 'INSUMO' ELSE 'PRODUTO_REVENDA' END)=:tipoItem
           AND ic.unidade_historica=:unidade
         ORDER BY item_nome LIMIT 1
        """,nativeQuery=true)
    Optional<OpcaoItemAnalise> buscarIdentidadeItemAnalise(@Param("tipoItem")String tipoItem,
            @Param("referenciaId")Long referenciaId,@Param("unidade")String unidade);
    @Query("""
        SELECT DISTINCT TRIM(c.fornecedor) FROM Compra c
         WHERE c.status=br.com.sergio.gestaopedidos.enums.StatusCompra.ATIVA
           AND c.fornecedor IS NOT NULL AND TRIM(c.fornecedor)<>''
           AND (:termo='' OR LOWER(c.fornecedor) LIKE LOWER(CONCAT('%',:termo,'%')))
         ORDER BY TRIM(c.fornecedor)
        """)
    java.util.List<String> buscarFornecedoresAnalise(@Param("termo")String termo,Pageable pageable);
    @EntityGraph(attributePaths={"itens","itens.insumo","itens.produto"}) @Query("SELECT DISTINCT c FROM Compra c WHERE c.id=:id") Optional<Compra> buscarDetalhada(@Param("id")Long id);
    @Query("""
        SELECT COUNT(c) AS quantidade,COALESCE(SUM(c.valorTotal),0) AS valorTotal,
        SUM(CASE WHEN c.tipoCompra=br.com.sergio.gestaopedidos.enums.TipoCompra.INSUMO THEN 1 ELSE 0 END) AS comprasInsumos,
        COALESCE(SUM(CASE WHEN c.tipoCompra=br.com.sergio.gestaopedidos.enums.TipoCompra.INSUMO THEN c.valorTotal ELSE 0 END),0) AS valorInsumos,
        SUM(CASE WHEN c.tipoCompra=br.com.sergio.gestaopedidos.enums.TipoCompra.PRODUTO_REVENDA THEN 1 ELSE 0 END) AS comprasRevenda,
        COALESCE(SUM(CASE WHEN c.tipoCompra=br.com.sergio.gestaopedidos.enums.TipoCompra.PRODUTO_REVENDA THEN c.valorTotal ELSE 0 END),0) AS valorRevenda
        FROM Compra c WHERE c.dataCompra=:data AND c.status<>br.com.sergio.gestaopedidos.enums.StatusCompra.ESTORNADA
        """) ResumoDashboard resumirDashboard(@Param("data")LocalDate data);
    @Query("""
        SELECT c.id AS id,c.tipoCompra AS tipoCompra,c.dataCompra AS dataCompra,c.fornecedor AS fornecedor,
               c.valorTotal AS valorTotal,COUNT(i.id) AS quantidadeItens
          FROM Compra c LEFT JOIN c.itens i
         WHERE c.status=br.com.sergio.gestaopedidos.enums.StatusCompra.ATIVA
         GROUP BY c.id,c.tipoCompra,c.dataCompra,c.fornecedor,c.valorTotal,c.atualizadoEm
         ORDER BY c.dataCompra DESC,c.atualizadoEm DESC,c.id DESC
        """)
    java.util.List<CompraRecente> buscarUltimasAtivas(Pageable pageable);
}
