package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Column(name = "horario_inicio")
    private LocalTime horarioInicio;

    @Column(name = "horario_fim")
    private LocalTime horarioFim;

    @Column(name = "endereco_entrega_historico", length = 150)
    private String enderecoEntregaHistorico;

    @Column(name = "numero_entrega_historico", length = 20)
    private String numeroEntregaHistorico;

    @Column(name = "bairro_entrega_historico", length = 100)
    private String bairroEntregaHistorico;

    @Column(name = "cidade_entrega_historico", length = 100)
    private String cidadeEntregaHistorico;

    @Column(name = "cep_entrega_historico", length = 10)
    private String cepEntregaHistorico;

    @Column(name = "complemento_entrega_historico", length = 150)
    private String complementoEntregaHistorico;

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

    @Column(name = "margem_bruta_estimada", precision = 9, scale = 4)
    private BigDecimal margemBrutaEstimada;

    @Column(length = 500)
    private String observacao;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Column(name = "estoque_movimentado")
    @Builder.Default
    private Boolean estoqueMovimentado = false;

    @Column(name = "estoque_movimentado_em")
    private LocalDateTime estoqueMovimentadoEm;

    @Column(name = "planejado_em")
    private LocalDateTime planejadoEm;

    @Column(name = "ordem_planejada")
    private Integer ordemPlanejada;

    @Column(name = "saida_sem_planejamento_em")
    private LocalDateTime saidaSemPlanejamentoEm;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @OneToMany(
            mappedBy = "pedido",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    @BatchSize(size = 50)
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

    public void fotografarEnderecoEntrega(Cliente cliente) {
        if (tipoEntrega != TipoEntrega.ENTREGA || cliente == null) {
            limparEnderecoEntrega();
            return;
        }
        enderecoEntregaHistorico = normalizar(cliente.getEndereco());
        numeroEntregaHistorico = normalizar(cliente.getNumero());
        bairroEntregaHistorico = normalizar(cliente.getBairro());
        cidadeEntregaHistorico = normalizar(cliente.getCidade());
        cepEntregaHistorico = normalizar(cliente.getCep());
        complementoEntregaHistorico = normalizar(cliente.getComplemento());
    }

    public void limparEnderecoEntrega() {
        enderecoEntregaHistorico = null;
        numeroEntregaHistorico = null;
        bairroEntregaHistorico = null;
        cidadeEntregaHistorico = null;
        cepEntregaHistorico = null;
        complementoEntregaHistorico = null;
    }

    public boolean isPlanejamentoConfirmado() {
        return planejadoEm != null && ordemPlanejada != null;
    }

    public void confirmarPlanejamento(LocalDateTime instante, int ordem) {
        planejadoEm = instante;
        ordemPlanejada = ordem;
        saidaSemPlanejamentoEm = null;
    }

    public void invalidarPlanejamento() {
        planejadoEm = null;
        ordemPlanejada = null;
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public void aplicarResultadoHistorico(BigDecimal cmv) {
        if (cmv == null || subtotal == null) {
            throw new IllegalArgumentException("Pedido inválido para consolidação do resultado histórico.");
        }
        custoTotalHistorico = cmv.setScale(2, RoundingMode.HALF_UP);
        lucroBrutoEstimado = subtotal.subtract(custoTotalHistorico).setScale(2, RoundingMode.HALF_UP);
        margemBrutaEstimada = subtotal.signum() == 0 ? null
                : lucroBrutoEstimado.multiply(BigDecimal.valueOf(100))
                        .divide(subtotal, 4, RoundingMode.HALF_UP);
    }
}
