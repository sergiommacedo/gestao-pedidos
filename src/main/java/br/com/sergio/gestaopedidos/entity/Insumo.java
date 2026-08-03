package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import jakarta.persistence.*;
import lombok.*;
import java.math.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "insumos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Insumo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false, length = 20)
    private UnidadeMedida unidadeMedida;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @Column(name = "estoque_minimo", nullable = false, precision = 15, scale = 3)
    @Builder.Default
    private BigDecimal estoqueMinimo = BigDecimal.ZERO.setScale(3);

    @Column(length = 500)
    private String observacao;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() {
        normalizarDefaults();
        criadoEm = LocalDateTime.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    void preUpdate() {
        normalizarDefaults();
        atualizadoEm = LocalDateTime.now();
    }

    private void normalizarDefaults() {
        if (ativo == null) ativo = true;
        estoqueMinimo = (estoqueMinimo == null ? BigDecimal.ZERO : estoqueMinimo)
                .setScale(3, RoundingMode.UNNECESSARY);
    }
}
