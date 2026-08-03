package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="movimentacoes_estoque",uniqueConstraints={
        @UniqueConstraint(name="uk_movimento_item_tipo",columnNames={"item_compra_id","tipo"}),
        @UniqueConstraint(name="uk_movimento_pedido_produto_tipo",columnNames={"pedido_id","produto_id","tipo"})})
@Check(constraints="(insumo_id is not null and produto_id is null and tipo_item='INSUMO') or (insumo_id is null and produto_id is not null and tipo_item in ('PRODUTO_REVENDA','PRODUTO_PRODUZIDO'))")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder @EqualsAndHashCode(onlyExplicitlyIncluded=true)
public class MovimentacaoEstoque {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @Enumerated(EnumType.STRING) @Column(name="tipo_item",nullable=false,length=30) private TipoItemEstoque tipoItem;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="insumo_id") private Insumo insumo;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="produto_id") private Produto produto;
    @Column(name="nome_historico",nullable=false,length=100) private String nomeHistorico;
    @Column(name="data_movimentacao",nullable=false) private LocalDateTime dataMovimentacao;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private TipoMovimentacaoEstoque tipo;
    @Column(nullable=false,precision=15,scale=3) private BigDecimal quantidade;
    @Enumerated(EnumType.STRING) @Column(name="unidade_historica",nullable=false,length=20) private UnidadeMedida unidadeHistorica;
    @Column(name="valor_total",nullable=false,precision=18,scale=2) private BigDecimal valorTotal;
    @Column(name="custo_unitario",nullable=false,precision=18,scale=6) private BigDecimal custoUnitario;
    @Column(name="saldo_anterior",precision=15,scale=3) private BigDecimal saldoAnterior;
    @Column(name="saldo_posterior",precision=15,scale=3) private BigDecimal saldoPosterior;
    @Column(name="usuario_responsavel",length=100) private String usuarioResponsavel;
    @Column(length=500) private String observacao;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="item_compra_id") private ItemCompra itemCompra;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="producao_id") private Producao producao;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="pedido_id") private Pedido pedido;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="movimentacao_origem_id") private MovimentacaoEstoque movimentacaoOrigem;
    @Column(name="criado_em",nullable=false,updatable=false) private LocalDateTime criadoEm;
    @PrePersist void criar(){if(dataMovimentacao==null)dataMovimentacao=LocalDateTime.now();criadoEm=LocalDateTime.now();}
}
