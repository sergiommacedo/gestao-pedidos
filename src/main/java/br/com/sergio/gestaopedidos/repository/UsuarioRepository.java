package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmailIgnoreCase(String email);
}