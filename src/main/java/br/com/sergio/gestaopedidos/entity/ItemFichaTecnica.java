package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "itens_ficha_tecnica", uniqueConstraints =
        @UniqueConstraint(name = "uk_item_ficha_insumo", columnNames = {"ficha_tecnica_id", "insumo_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemFichaTecnica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ficha_tecnica_id", nullable = false)
    private FichaTecnica fichaTecnica;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_historica", nullable = false, length = 20)
    private UnidadeMedida unidadeHistorica;

    @Column(name = "nome_historico", nullable = false, length = 100)
    private String nomeHistorico;
}
