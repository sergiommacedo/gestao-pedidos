package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dataPedido;

    @Column(nullable = false)
    private LocalDate dataAgendada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEntrega tipoEntrega;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxaEntrega;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "custo_total_historico", precision = 18, scale = 2)
    private BigDecimal custoTotalHistorico;

    @Column(name = "lucro_bruto_estimado", precision = 18, scale = 2)
    private BigDecimal lucroBrutoEstimado;

    @Column(length = 500)
    private String observacao;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "estoque_movimentado")
    @Builder.Default
    private Boolean estoqueMovimentado = false;

    @Column(name = "estoque_movimentado_em")
    private LocalDateTime estoqueMovimentadoEm;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(
            mappedBy = "pedido",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (dataPedido == null) {
            dataPedido = LocalDateTime.now();
        }

        if (status == null) {
            status = StatusPedido.PENDENTE;
        }

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        if (taxaEntrega == null) {
            taxaEntrega = BigDecimal.ZERO;
        }
        if (estoqueMovimentado == null) estoqueMovimentado = false;

        calcularValorTotal();
    }

    @PreUpdate
    public void preUpdate() {
        calcularValorTotal();
    }

    public void calcularValorTotal() {
        BigDecimal subtotalSeguro =
                subtotal != null ? subtotal : BigDecimal.ZERO;

        BigDecimal taxaEntregaSegura =
                taxaEntrega != null ? taxaEntrega : BigDecimal.ZERO;

        valorTotal = subtotalSeguro.add(taxaEntregaSegura);
    }
}
