package br.com.sergio.gestaopedidos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import br.com.sergio.gestaopedidos.enums.StatusProducao;

@Entity
@Table(name = "producoes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Producao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "data_producao", nullable = false)
    private LocalDate dataProducao;
    @Enumerated(EnumType.STRING) @Column(length=20) @Builder.Default private StatusProducao status=StatusProducao.RASCUNHO;
    @Column(name="confirmada_em") private LocalDateTime confirmadaEm;
    @OneToMany(mappedBy="producao",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id") @Builder.Default
    private List<ItemProducao> itens=new ArrayList<>();

    @Column(name = "valor_gas_energia", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorGasEnergia;
    @Column(name = "valor_outros", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorOutros;
    @Column(name="valor_insumos_consumidos",nullable=false,precision=18,scale=2)
    @Builder.Default private BigDecimal valorInsumosConsumidos=BigDecimal.ZERO.setScale(2);
    @Column(name="custo_total",nullable=false,precision=18,scale=2)
    @Builder.Default private BigDecimal custoTotal=BigDecimal.ZERO.setScale(2);
    @Column(length = 500)
    private String observacao;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    void prePersist() { normalizarValores(); if(status==null)status=StatusProducao.RASCUNHO; criadoEm = LocalDateTime.now(); atualizadoEm = criadoEm; }
    @PreUpdate
    void preUpdate() { normalizarValores(); atualizadoEm = LocalDateTime.now(); }

    public void normalizarValores() {
        valorGasEnergia = moeda(valorGasEnergia); valorOutros = moeda(valorOutros);
        valorInsumosConsumidos=moeda(valorInsumosConsumidos);custoTotal=moeda(custoTotal);
    }
    private BigDecimal moeda(BigDecimal valor) { return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP); }
    public void adicionarItem(ItemProducao item){itens.add(item);item.setProducao(this);}
    public void limparItens(){itens.forEach(i->i.setProducao(null));itens.clear();}
    public StatusProducao statusEfetivo(){return status==null?StatusProducao.RASCUNHO:status;}
}
