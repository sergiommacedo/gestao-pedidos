package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Usuario;
import br.com.sergio.gestaopedidos.enums.PerfilUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByPerfilAndAtivoTrue(PerfilUsuario perfil);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    Page<Usuario> findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String nome,
            String email,
            Pageable pageable
    );
}
