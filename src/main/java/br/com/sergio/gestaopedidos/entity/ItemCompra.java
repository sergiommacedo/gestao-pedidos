package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import java.math.BigDecimal;

@Entity @Table(name="itens_compra",uniqueConstraints={
        @UniqueConstraint(name="uk_item_compra_insumo",columnNames={"compra_id","insumo_id"}),
        @UniqueConstraint(name="uk_item_compra_produto",columnNames={"compra_id","produto_id"})})
@Check(constraints="(insumo_id is not null and produto_id is null) or (insumo_id is null and produto_id is not null)")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded=true)
public class ItemCompra {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="compra_id",nullable=false) private Compra compra;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="insumo_id") private Insumo insumo;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="produto_id") private Produto produto;
    @Column(name="nome_historico",nullable=false,length=100) private String nomeHistorico;
    @Enumerated(EnumType.STRING) @Column(name="unidade_historica",nullable=false,length=20) private UnidadeMedida unidadeHistorica;
    @Column(nullable=false,precision=15,scale=3) private BigDecimal quantidade;
    @Column(name="valor_total_item",nullable=false,precision=15,scale=2) private BigDecimal valorTotalItem;
    @Column(name="custo_unitario",nullable=false,precision=18,scale=6) private BigDecimal custoUnitario;

    @Transient
    public TipoItemEstoque getTipoItem(){
        if(insumo!=null&&produto==null)return TipoItemEstoque.INSUMO;
        if(produto!=null&&insumo==null&&produto.getTipoProduto()==TipoProduto.PRODUTO_REVENDA)return TipoItemEstoque.PRODUTO_REVENDA;
        throw new IllegalStateException("Origem do item da compra inválida.");
    }
}
