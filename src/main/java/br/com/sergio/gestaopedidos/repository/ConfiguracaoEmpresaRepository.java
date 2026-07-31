package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.ConfiguracaoEmpresa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoEmpresaRepository
        extends JpaRepository<ConfiguracaoEmpresa, Long> {
}
