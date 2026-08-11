package br.com.sergio.gestaopedidos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;

@Entity
@Table(name = "itens_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidade;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "nome_historico", length = 100)
    private String nomeHistorico;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_historica", length = 20)
    private UnidadeVenda unidadeHistorica;

    @Column(name = "custo_unitario_historico", precision = 18, scale = 6)
    private BigDecimal custoUnitarioHistorico;

    @Column(name = "custo_total_historico", precision = 18, scale = 2)
    private BigDecimal custoTotalHistorico;

    @Column(name = "lucro_bruto_historico", precision = 18, scale = 2)
    private BigDecimal lucroBrutoHistorico;

    @Column(name = "margem_bruta_historica", precision = 9, scale = 4)
    private BigDecimal margemBrutaHistorica;

    @Column(length = 255)
    private String observacao;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @PrePersist
    @PreUpdate
    public void calcularSubtotal() {
        if (quantidade != null && precoUnitario != null) {
            subtotal = precoUnitario.multiply(quantidade)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    public BigDecimal lucroBrutoEstimado() {
        return lucroBrutoHistorico;
    }

    public void aplicarCustoHistorico(BigDecimal custoTotal) {
        if (custoTotal == null || quantidade == null || quantidade.signum() <= 0 || subtotal == null) {
            throw new IllegalArgumentException("Item inválido para consolidação do custo histórico.");
        }
        custoTotalHistorico = custoTotal.setScale(2, RoundingMode.HALF_UP);
        custoUnitarioHistorico = custoTotalHistorico.divide(quantidade, 6, RoundingMode.HALF_UP);
        lucroBrutoHistorico = subtotal.subtract(custoTotalHistorico).setScale(2, RoundingMode.HALF_UP);
        margemBrutaHistorica = subtotal.signum() == 0 ? null
                : lucroBrutoHistorico.multiply(BigDecimal.valueOf(100))
                        .divide(subtotal, 4, RoundingMode.HALF_UP);
    }
}
