package br.com.sergio.gestaopedidos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fichas_tecnicas", uniqueConstraints =
        @UniqueConstraint(name = "uk_ficha_tecnica_produto", columnNames = "produto_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FichaTecnica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, updatable = false)
    private Produto produto;

    @Column(name = "rendimento_esperado", nullable = false, precision = 15, scale = 3)
    private BigDecimal rendimentoEsperado;

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativa = true;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "fichaTecnica", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @Builder.Default
    private List<ItemFichaTecnica> itens = new ArrayList<>();

    public void adicionarItem(ItemFichaTecnica item) {
        itens.add(item);
        item.setFichaTecnica(this);
    }

    public void removerItem(ItemFichaTecnica item) {
        itens.remove(item);
        item.setFichaTecnica(null);
    }

    @PrePersist
    void prePersist() {
        if (ativa == null) ativa = true;
        criadoEm = LocalDateTime.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    void preUpdate() { atualizadoEm = LocalDateTime.now(); }
}
