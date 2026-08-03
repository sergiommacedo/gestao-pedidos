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

    List<Produto> findTop20ByAtivoTrueAndVendavelTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(
            String nome
    );

    List<Produto> findByAtivoTrueOrderByNomeAsc();

    List<Produto> findByTipoProdutoOrderByNomeAsc(TipoProduto tipoProduto);

    List<Produto> findByTipoProdutoAndAtivoTrueOrderByNomeAsc(TipoProduto tipoProduto);

    List<Produto> findTop20ByTipoProdutoAndAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(TipoProduto tipoProduto, String nome);

    @Query("SELECT p FROM Produto p JOIN FichaTecnica f ON f.produto=p WHERE p.tipoProduto=br.com.sergio.gestaopedidos.enums.TipoProduto.PRODUZIDO AND p.ativo=true AND f.ativa=true ORDER BY p.nome")
    List<Produto> buscarProduzidosAtivosComFichaAtiva();
}
