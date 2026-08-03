package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Check;
import java.math.BigDecimal;

@Entity
@Table(name="itens_composicao_produto",uniqueConstraints={
        @UniqueConstraint(name="uk_composicao_insumo",columnNames={"composicao_id","insumo_id"}),
        @UniqueConstraint(name="uk_composicao_preparacao",columnNames={"composicao_id","preparacao_id"})})
@Check(constraints="(tipo_componente='INSUMO' and insumo_id is not null and preparacao_id is null) or (tipo_componente='PREPARACAO_PRODUZIDA' and insumo_id is null and preparacao_id is not null)")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded=true)
public class ItemComposicaoProduto {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="composicao_id",nullable=false) private ComposicaoProduto composicao;
    @Enumerated(EnumType.STRING) @Column(name="tipo_componente",nullable=false,length=30) private TipoComponenteComposicao tipoComponente;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="insumo_id") private Insumo insumo;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="preparacao_id") private Produto preparacao;
    @Column(name="nome_historico",nullable=false,length=100) private String nomeHistorico;
    @Enumerated(EnumType.STRING) @Column(name="unidade_historica",nullable=false,length=20) private UnidadeMedida unidadeHistorica;
    @Column(nullable=false,precision=15,scale=3) private BigDecimal quantidade;
}
