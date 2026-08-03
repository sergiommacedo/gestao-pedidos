package br.com.sergio.gestaopedidos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.*;
import java.time.*;
import java.util.*;
import br.com.sergio.gestaopedidos.enums.StatusCompraInsumo;

@Entity @Table(name="compras_insumos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded=true)
public class CompraInsumo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @Column(name="data_compra",nullable=false) private LocalDate dataCompra;
    @Column(length=150) private String fornecedor;
    @Column(length=500) private String observacao;
    @Column(name="valor_total",nullable=false,precision=15,scale=2) @Builder.Default private BigDecimal valorTotal=BigDecimal.ZERO.setScale(2);
    @Column(name="criado_em",nullable=false,updatable=false) private LocalDateTime criadoEm;
    @Column(name="atualizado_em",nullable=false) private LocalDateTime atualizadoEm;
    @OneToMany(mappedBy="compra",cascade=CascadeType.ALL,orphanRemoval=true) @Builder.Default
    private List<ItemCompraInsumo> itens=new ArrayList<>();
    @Enumerated(EnumType.STRING) @Column(length=20) @Builder.Default private StatusCompraInsumo status=StatusCompraInsumo.ATIVA;

    public void adicionarItem(ItemCompraInsumo item){item.setCompra(this);itens.add(item);}
    public void removerItem(ItemCompraInsumo item){itens.remove(item);item.setCompra(null);}
    public void recalcularTotal(){valorTotal=itens.stream().map(ItemCompraInsumo::getValorTotalItem).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);}
    @PrePersist void prePersist(){if(status==null)status=StatusCompraInsumo.ATIVA;recalcularTotal();criadoEm=LocalDateTime.now();atualizadoEm=criadoEm;}
    @PreUpdate void preUpdate(){recalcularTotal();atualizadoEm=LocalDateTime.now();}
}
