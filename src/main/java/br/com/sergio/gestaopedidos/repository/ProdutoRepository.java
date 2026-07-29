package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

}