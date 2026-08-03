package br.com.sergio.gestaopedidos.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name="composicoes_produtos", uniqueConstraints=@UniqueConstraint(name="uk_composicao_produto", columnNames="produto_comercial_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded=true)
public class ComposicaoProduto {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @EqualsAndHashCode.Include private Long id;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="produto_comercial_id",nullable=false,updatable=false) private Produto produtoComercial;
    @Column(nullable=false) @Builder.Default private Boolean ativa=true;
    @Column(length=500) private String observacao;
    @Column(name="criado_em",nullable=false,updatable=false) private LocalDateTime criadoEm;
    @Column(name="atualizado_em",nullable=false) private LocalDateTime atualizadoEm;
    @OneToMany(mappedBy="composicao",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id") @Builder.Default private List<ItemComposicaoProduto> itens=new ArrayList<>();
    public void adicionarItem(ItemComposicaoProduto item){item.setComposicao(this);itens.add(item);}
    @PrePersist void criar(){if(ativa==null)ativa=true;criadoEm=LocalDateTime.now();atualizadoEm=criadoEm;}
    @PreUpdate void atualizar(){atualizadoEm=LocalDateTime.now();}
}
