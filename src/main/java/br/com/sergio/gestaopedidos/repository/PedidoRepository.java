package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

}