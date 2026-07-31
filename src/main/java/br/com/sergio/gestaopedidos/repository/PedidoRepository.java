package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Pedido;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("""
            SELECT p
            FROM Pedido p
            JOIN p.cliente c
            WHERE (
                :filtro = ''
                OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :filtro, '%'))
                OR c.telefone LIKE CONCAT('%', :filtro, '%')
            )
            AND (:status IS NULL OR p.status = :status)
            AND (:dataAgendada IS NULL OR p.dataAgendada = :dataAgendada)
            """)
    Page<Pedido> buscarPaginado(
            @Param("filtro") String filtro,
            @Param("status") StatusPedido status,
            @Param("dataAgendada") LocalDate dataAgendada,
            Pageable pageable
    );

    long countByDataAgendadaAndStatusNot(
            LocalDate dataAgendada,
            StatusPedido status
    );
}
