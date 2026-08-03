package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
