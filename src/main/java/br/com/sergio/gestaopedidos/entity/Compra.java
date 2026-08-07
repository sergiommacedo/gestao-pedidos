package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Entity @Table(name = "compras")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Compra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @Enumerated(EnumType.STRING) @Column(name="tipo_compra",nullable=false,length=30,updatable=false) private TipoCompra tipoCompra;
    @Column(name="data_compra",nullable=false) private LocalDate dataCompra;
    @Column(length=150) private String fornecedor;
    @Column(length=500) private String observacao;
    @Column(name="valor_total",nullable=false,precision=15,scale=2) @Builder.Default private BigDecimal valorTotal=BigDecimal.ZERO.setScale(2);
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) @Builder.Default private StatusCompra status=StatusCompra.ATIVA;
    @Column(name="criado_em",nullable=false,updatable=false) private LocalDateTime criadoEm;
    @Column(name="atualizado_em",nullable=false) private LocalDateTime atualizadoEm;
    @OneToMany(mappedBy="compra",cascade=CascadeType.ALL,orphanRemoval=true) @Builder.Default private List<ItemCompra> itens=new ArrayList<>();

    public void adicionarItem(ItemCompra item){item.setCompra(this);itens.add(item);}
    public void recalcularTotal(){valorTotal=itens.stream().map(ItemCompra::getValorTotalItem).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2,RoundingMode.HALF_UP);}
    public void recalcularTipoCompra(){
        boolean insumo=itens.stream().anyMatch(i->i.getTipoItem()==TipoItemEstoque.INSUMO);
        boolean revenda=itens.stream().anyMatch(i->i.getTipoItem()==TipoItemEstoque.PRODUTO_REVENDA);
        tipoCompra=insumo&&revenda?TipoCompra.MISTA:insumo?TipoCompra.INSUMO:revenda?TipoCompra.PRODUTO_REVENDA:null;
    }
    @PrePersist void criar(){if(status==null)status=StatusCompra.ATIVA;recalcularTipoCompra();recalcularTotal();criadoEm=LocalDateTime.now();atualizadoEm=criadoEm;}
    @PreUpdate void atualizar(){recalcularTipoCompra();recalcularTotal();atualizadoEm=LocalDateTime.now();}
}
