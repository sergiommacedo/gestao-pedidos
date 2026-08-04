package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import br.com.sergio.gestaopedidos.enums.TipoProduto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    boolean existsByNomeIgnoreCase(String nome);

    Page<Produto> findByNomeContainingIgnoreCaseOrDescricaoContainingIgnoreCase(
            String nome,
            String descricao,
            Pageable pageable
    );

    @Query("SELECT p FROM Produto p WHERE p.ativo=true AND p.vendavel=true AND p.tipoProduto IN :tipos AND LOWER(p.nome) LIKE LOWER(CONCAT('%',:nome,'%')) ORDER BY p.nome")
    List<Produto> buscarVendaveis(@org.springframework.data.repository.query.Param("tipos") java.util.Collection<TipoProduto> tipos,@org.springframework.data.repository.query.Param("nome") String nome,org.springframework.data.domain.Pageable pageable);

    List<Produto> findByAtivoTrueOrderByNomeAsc();

    List<Produto> findByTipoProdutoOrderByNomeAsc(TipoProduto tipoProduto);

    List<Produto> findByTipoProdutoAndAtivoTrueOrderByNomeAsc(TipoProduto tipoProduto);

    List<Produto> findTop20ByTipoProdutoAndAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(TipoProduto tipoProduto, String nome);

    @Query("SELECT DISTINCT p FROM Produto p JOIN FichaTecnica f ON f.produto=p JOIN f.itens i WHERE p.tipoProduto=br.com.sergio.gestaopedidos.enums.TipoProduto.PREPARACAO_PRODUZIDA AND p.ativo=true AND f.ativa=true ORDER BY p.nome")
    List<Produto> buscarProduzidosAtivosComFichaAtiva();

    @Query("SELECT p FROM Produto p WHERE (:tipo IS NULL OR p.tipoProduto=:tipo) AND (:ativo IS NULL OR p.ativo=:ativo) AND (:vendavel IS NULL OR p.vendavel=:vendavel) AND (:filtro='' OR LOWER(p.nome) LIKE LOWER(CONCAT('%',:filtro,'%')) OR LOWER(COALESCE(p.descricao,'')) LIKE LOWER(CONCAT('%',:filtro,'%')))")
    Page<Produto> listar(@org.springframework.data.repository.query.Param("filtro") String filtro,@org.springframework.data.repository.query.Param("tipo") TipoProduto tipo,@org.springframework.data.repository.query.Param("ativo") Boolean ativo,@org.springframework.data.repository.query.Param("vendavel") Boolean vendavel,Pageable pageable);
}
