package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Page<Cliente> findByNomeContainingIgnoreCaseOrTelefoneContainingIgnoreCase(
            String nome,
            String telefone,
            Pageable pageable
    );
}