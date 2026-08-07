package br.com.sergio.gestaopedidos.repository;
import br.com.sergio.gestaopedidos.entity.*;import br.com.sergio.gestaopedidos.enums.TipoItemEstoque;import jakarta.persistence.LockModeType;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;import java.math.*;import java.time.LocalDateTime;import java.util.*;
public interface SaldoEstoqueRepository extends JpaRepository<SaldoEstoque,Long>{
 interface InsumoCusto{Long getId();String getNome();String getUnidade();BigDecimal getCustoMedio();BigDecimal getEstoqueAtual();}
 interface Visao{String getTipoItem();Long getReferenciaId();String getItemNome();String getUnidade();Boolean getAtivo();BigDecimal getQuantidadeAtual();BigDecimal getEstoqueMinimo();BigDecimal getCustoMedioAtual();BigDecimal getValorTotalEstoque();LocalDateTime getAtualizadoEm();}
 interface ResumoDashboard{Long getItensComSaldo();Long getAbaixoDoMinimo();Long getSemEstoque();Long getProduzidosDisponiveis();Long getRevendaDisponiveis();}
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("SELECT i FROM Insumo i WHERE i.id=:id")Optional<Insumo> bloquearInsumo(@Param("id")Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("SELECT p FROM Produto p WHERE p.id=:id")Optional<Produto> bloquearProduto(@Param("id")Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("SELECT s FROM SaldoEstoque s WHERE s.tipoItem=:tipo AND ((:tipo=br.com.sergio.gestaopedidos.enums.TipoItemEstoque.INSUMO AND s.insumo.id=:id) OR (:tipo<>br.com.sergio.gestaopedidos.enums.TipoItemEstoque.INSUMO AND s.produto.id=:id))")Optional<SaldoEstoque> bloquearSaldo(@Param("tipo")TipoItemEstoque tipo,@Param("id")Long id);
 @Query("SELECT s FROM SaldoEstoque s WHERE s.tipoItem=:tipo AND ((:tipo=br.com.sergio.gestaopedidos.enums.TipoItemEstoque.INSUMO AND s.insumo.id=:id) OR (:tipo<>br.com.sergio.gestaopedidos.enums.TipoItemEstoque.INSUMO AND s.produto.id=:id))")Optional<SaldoEstoque> buscarSaldo(@Param("tipo")TipoItemEstoque tipo,@Param("id")Long id);
 @Query(value="""
 SELECT * FROM (
 SELECT 'INSUMO' tipo_item,i.id referencia_id,i.nome item_nome,i.unidade_medida unidade,i.ativo ativo,
 COALESCE(s.quantidade_atual,0) quantidade_atual,COALESCE(i.estoque_minimo,0) estoque_minimo,
 COALESCE(s.custo_medio_atual,0) custo_medio_atual,COALESCE(s.valor_total_estoque,0) valor_total_estoque,s.atualizado_em atualizado_em
 FROM insumos i LEFT JOIN saldos_estoque s ON s.insumo_id=i.id
 UNION ALL
 SELECT 'PRODUTO_REVENDA',p.id,p.nome,p.unidade_venda,p.ativo,
 COALESCE(s.quantidade_atual,0),COALESCE(p.estoque_minimo,0),COALESCE(s.custo_medio_atual,0),COALESCE(s.valor_total_estoque,0),s.atualizado_em
 FROM produtos p LEFT JOIN saldos_estoque s ON s.produto_id=p.id WHERE p.tipo_produto='PRODUTO_REVENDA'
 UNION ALL
 SELECT 'PREPARACAO_PRODUZIDA',p.id,p.nome,p.unidade_venda,p.ativo,
 COALESCE(s.quantidade_atual,0),COALESCE(p.estoque_minimo,0),COALESCE(s.custo_medio_atual,0),COALESCE(s.valor_total_estoque,0),s.atualizado_em
 FROM produtos p LEFT JOIN saldos_estoque s ON s.produto_id=p.id WHERE p.tipo_produto='PREPARACAO_PRODUZIDA'
 ) x WHERE (:nome='' OR LOWER(x.item_nome) LIKE LOWER(CONCAT('%',:nome,'%')))
 AND (:categoria='' OR x.tipo_item=:categoria) AND (:ativo IS NULL OR x.ativo=:ativo)
 AND (:situacao='' OR (:situacao='SEM_ESTOQUE' AND x.quantidade_atual<=0)
 OR (:situacao='BAIXO' AND x.quantidade_atual>0 AND x.estoque_minimo>0 AND x.quantidade_atual<x.estoque_minimo)
 OR (:situacao='SEM_MINIMO' AND x.estoque_minimo=0)
 OR (:situacao='NORMAL' AND x.quantidade_atual>0 AND (x.estoque_minimo=0 OR x.quantidade_atual>=x.estoque_minimo)))
 ORDER BY x.item_nome
 """,countQuery="""
 SELECT COUNT(*) FROM (
 SELECT 'INSUMO' tipo_item,i.nome item_nome,i.ativo ativo,COALESCE(s.quantidade_atual,0) quantidade_atual,COALESCE(i.estoque_minimo,0) estoque_minimo FROM insumos i LEFT JOIN saldos_estoque s ON s.insumo_id=i.id
 UNION ALL SELECT 'PRODUTO_REVENDA',p.nome,p.ativo,COALESCE(s.quantidade_atual,0),COALESCE(p.estoque_minimo,0) FROM produtos p LEFT JOIN saldos_estoque s ON s.produto_id=p.id WHERE p.tipo_produto='PRODUTO_REVENDA'
 UNION ALL SELECT 'PREPARACAO_PRODUZIDA',p.nome,p.ativo,COALESCE(s.quantidade_atual,0),COALESCE(p.estoque_minimo,0) FROM produtos p LEFT JOIN saldos_estoque s ON s.produto_id=p.id WHERE p.tipo_produto='PREPARACAO_PRODUZIDA') x
 WHERE (:nome='' OR LOWER(x.item_nome) LIKE LOWER(CONCAT('%',:nome,'%'))) AND (:categoria='' OR x.tipo_item=:categoria) AND (:ativo IS NULL OR x.ativo=:ativo)
 AND (:situacao='' OR (:situacao='SEM_ESTOQUE' AND x.quantidade_atual<=0) OR (:situacao='BAIXO' AND x.quantidade_atual>0 AND x.estoque_minimo>0 AND x.quantidade_atual<x.estoque_minimo) OR (:situacao='SEM_MINIMO' AND x.estoque_minimo=0) OR (:situacao='NORMAL' AND x.quantidade_atual>0 AND (x.estoque_minimo=0 OR x.quantidade_atual>=x.estoque_minimo)))
 """,nativeQuery=true)Page<Visao> listar(@Param("nome")String nome,@Param("categoria")String categoria,@Param("situacao")String situacao,@Param("ativo")Boolean ativo,Pageable pageable);
 @Query("SELECT COUNT(s) FROM SaldoEstoque s WHERE s.quantidadeAtual>0")long contarComSaldo();
 @Query(value="""
 SELECT COALESCE(SUM(s.valor_total_estoque),0) FROM saldos_estoque s
 WHERE s.tipo_item=:#{#tipo.name()} AND (
   (s.insumo_id IS NOT NULL AND EXISTS (SELECT 1 FROM insumos i WHERE i.id=s.insumo_id AND i.ativo=true)) OR
   (s.produto_id IS NOT NULL AND EXISTS (SELECT 1 FROM produtos p WHERE p.id=s.produto_id AND p.ativo=true))
 )
 """,nativeQuery=true)BigDecimal somarValor(@Param("tipo")TipoItemEstoque tipo);
 @Query(value="""
 SELECT COALESCE(SUM(CASE WHEN x.quantidade_atual>0 THEN 1 ELSE 0 END),0) itens_com_saldo,
        COALESCE(SUM(CASE WHEN x.quantidade_atual>0 AND x.estoque_minimo>0 AND x.quantidade_atual<x.estoque_minimo THEN 1 ELSE 0 END),0) abaixo_do_minimo,
        COALESCE(SUM(CASE WHEN x.quantidade_atual<=0 THEN 1 ELSE 0 END),0) sem_estoque,
        COALESCE(SUM(CASE WHEN x.tipo_item='PREPARACAO_PRODUZIDA' AND x.quantidade_atual>0 THEN 1 ELSE 0 END),0) produzidos_disponiveis,
        COALESCE(SUM(CASE WHEN x.tipo_item='PRODUTO_REVENDA' AND x.quantidade_atual>0 THEN 1 ELSE 0 END),0) revenda_disponiveis
 FROM (
   SELECT 'INSUMO' tipo_item,i.ativo,COALESCE(s.quantidade_atual,0) quantidade_atual,COALESCE(i.estoque_minimo,0) estoque_minimo
     FROM insumos i LEFT JOIN saldos_estoque s ON s.insumo_id=i.id
   UNION ALL
   SELECT 'PRODUTO_REVENDA',p.ativo,COALESCE(s.quantidade_atual,0),COALESCE(p.estoque_minimo,0)
     FROM produtos p LEFT JOIN saldos_estoque s ON s.produto_id=p.id WHERE p.tipo_produto='PRODUTO_REVENDA'
   UNION ALL
   SELECT 'PREPARACAO_PRODUZIDA',p.ativo,COALESCE(s.quantidade_atual,0),COALESCE(p.estoque_minimo,0)
     FROM produtos p LEFT JOIN saldos_estoque s ON s.produto_id=p.id WHERE p.tipo_produto='PREPARACAO_PRODUZIDA'
 ) x WHERE x.ativo=true
 """,nativeQuery=true) ResumoDashboard resumirDashboard();
 @Query(value="""
 SELECT * FROM (
 SELECT 'INSUMO' tipo_item,i.id referencia_id,i.nome item_nome,i.unidade_medida unidade,i.ativo ativo,COALESCE(s.quantidade_atual,0) quantidade_atual,COALESCE(i.estoque_minimo,0) estoque_minimo,COALESCE(s.custo_medio_atual,0) custo_medio_atual,COALESCE(s.valor_total_estoque,0) valor_total_estoque,s.atualizado_em atualizado_em FROM insumos i LEFT JOIN saldos_estoque s ON s.insumo_id=i.id
 UNION ALL SELECT CASE WHEN p.tipo_produto='PREPARACAO_PRODUZIDA' THEN 'PREPARACAO_PRODUZIDA' ELSE 'PRODUTO_REVENDA' END,p.id,p.nome,p.unidade_venda,p.ativo,COALESCE(s.quantidade_atual,0),COALESCE(p.estoque_minimo,0),COALESCE(s.custo_medio_atual,0),COALESCE(s.valor_total_estoque,0),s.atualizado_em FROM produtos p LEFT JOIN saldos_estoque s ON s.produto_id=p.id WHERE p.tipo_produto IN ('PREPARACAO_PRODUZIDA','PRODUTO_REVENDA')
 ) x WHERE x.ativo=true AND (x.quantidade_atual<=0 OR (x.quantidade_atual>0 AND x.estoque_minimo>0 AND x.quantidade_atual<x.estoque_minimo))
 ORDER BY CASE WHEN x.quantidade_atual<=0 THEN 0 ELSE 1 END,x.quantidade_atual ASC,x.item_nome ASC
 """,nativeQuery=true) List<Visao> listarAlertasDashboard(Pageable pageable);
 @Query(value="""
 SELECT 'PREPARACAO_PRODUZIDA' tipo_item,p.id referencia_id,p.nome item_nome,p.unidade_venda unidade,p.ativo ativo,s.quantidade_atual quantidade_atual,COALESCE(p.estoque_minimo,0) estoque_minimo,s.custo_medio_atual custo_medio_atual,s.valor_total_estoque valor_total_estoque,s.atualizado_em atualizado_em
 FROM saldos_estoque s JOIN produtos p ON p.id=s.produto_id
 WHERE s.tipo_item='PREPARACAO_PRODUZIDA' AND p.ativo=true AND s.quantidade_atual>0
 ORDER BY s.quantidade_atual DESC,p.nome ASC
 """,nativeQuery=true) List<Visao> listarProduzidosDisponiveisDashboard(Pageable pageable);
 @Query("SELECT s FROM SaldoEstoque s JOIN FETCH s.insumo WHERE s.tipoItem=br.com.sergio.gestaopedidos.enums.TipoItemEstoque.INSUMO AND s.insumo.id IN :ids")List<SaldoEstoque> buscarSaldosInsumos(@Param("ids")Collection<Long> ids);
 @Query(value="""
 SELECT i.id id,i.nome nome,i.unidade_medida unidade,COALESCE(s.custo_medio_atual,0) custo_medio,COALESCE(s.quantidade_atual,0) estoque_atual
 FROM insumos i LEFT JOIN saldos_estoque s ON s.insumo_id=i.id
 WHERE i.ativo=true AND LOWER(i.nome) LIKE LOWER(CONCAT('%',:termo,'%')) ORDER BY i.nome LIMIT 20
 """,nativeQuery=true)List<InsumoCusto> buscarInsumosAtivosComCusto(@Param("termo")String termo);
}
