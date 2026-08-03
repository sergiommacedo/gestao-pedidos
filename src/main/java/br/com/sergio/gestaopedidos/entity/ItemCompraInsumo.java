package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name="itens_compra_insumo",uniqueConstraints=@UniqueConstraint(name="uk_compra_insumo",columnNames={"compra_id","insumo_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded=true)
public class ItemCompraInsumo {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="compra_id",nullable=false) private CompraInsumo compra;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="insumo_id",nullable=false) private Insumo insumo;
    @Column(nullable=false,precision=15,scale=3) private BigDecimal quantidade;
    @Enumerated(EnumType.STRING) @Column(name="unidade_medida",nullable=false,length=20) private UnidadeMedida unidadeMedida;
    @Column(name="valor_total_item",nullable=false,precision=15,scale=2) private BigDecimal valorTotalItem;
    @Column(name="custo_unitario",nullable=false,precision=18,scale=6) private BigDecimal custoUnitario;
}
