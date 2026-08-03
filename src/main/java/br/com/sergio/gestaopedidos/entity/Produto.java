package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.enums.TipoProduto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_venda", nullable = false, length = 20)
    private UnidadeVenda unidadeVenda;

    @Column(name = "permite_acompanhamento", nullable = false)
    @Builder.Default
    private Boolean permiteAcompanhamento = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_produto", length = 30)
    private TipoProduto tipoProduto;

    @Column
    @Builder.Default
    private Boolean vendavel = true;

    @Column(name = "estoque_minimo", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal estoqueMinimo = BigDecimal.ZERO.setScale(3);

    @PrePersist
    public void aplicarDefaults() {
        if (vendavel == null) vendavel = false;
        if (estoqueMinimo == null) estoqueMinimo = BigDecimal.ZERO.setScale(3);
    }
}
