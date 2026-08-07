package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.properties.hibernate.generate_statistics=true"})
class CompraAnaliseRepositoryTest {
    @Autowired CompraRepository repository;
    @Autowired EntityManager em;
    private Insumo feijao;
    private Produto refrigerante;

    @BeforeEach void preparar() {
        feijao=Insumo.builder().nome("Feijão").unidadeMedida(UnidadeMedida.QUILOGRAMA).build();em.persist(feijao);
        refrigerante=Produto.builder().nome("Refrigerante").tipoProduto(TipoProduto.PRODUTO_REVENDA)
                .unidadeVenda(UnidadeVenda.UNIDADE).build();em.persist(refrigerante);
    }

    @Test void resumoConsideraSomenteAtivasHojeMesEPeriodo() {
        compra(LocalDate.of(2026,8,7),"Rio Verde",StatusCompra.ATIVA,item(feijao,"2","20",UnidadeMedida.QUILOGRAMA));
        compra(LocalDate.of(2026,8,7),"Sereno",StatusCompra.ATIVA,item(refrigerante,"3","30",UnidadeMedida.UNIDADE));
        compra(LocalDate.of(2026,8,2),"Rio Verde",StatusCompra.ATIVA,item(feijao,"4","32",UnidadeMedida.QUILOGRAMA));
        compra(LocalDate.of(2026,7,30),"Antiga",StatusCompra.ATIVA,item(feijao,"1","7",UnidadeMedida.QUILOGRAMA));
        compra(LocalDate.of(2026,8,7),"Estornada",StatusCompra.ESTORNADA,item(feijao,"10","999",UnidadeMedida.QUILOGRAMA));
        em.flush();

        var r=repository.resumirOperacao(LocalDate.of(2026,8,7),LocalDate.of(2026,8,1),LocalDate.of(2026,8,31),
                LocalDate.of(2026,8,2),LocalDate.of(2026,8,7));
        assertThat(r.getComprasHoje()).isEqualTo(2);
        assertThat(r.getTotalHoje()).isEqualByComparingTo("50.00");
        assertThat(r.getTotalMes()).isEqualByComparingTo("82.00");
        assertThat(r.getTotalPeriodo()).isEqualByComparingTo("82.00");
    }

    @Test void agregaHistoricoFiltraESeparaFornecedorCategoriaEUnidade() {
        compra(LocalDate.of(2026,8,1),"Rio Verde",StatusCompra.ATIVA,item(feijao,"10","40",UnidadeMedida.QUILOGRAMA));
        compra(LocalDate.of(2026,8,5),"Rio Verde",StatusCompra.ATIVA,item(feijao,"20","100",UnidadeMedida.QUILOGRAMA));
        compra(LocalDate.of(2026,8,4),"Atacadão",StatusCompra.ATIVA,item(feijao,"10","90",UnidadeMedida.QUILOGRAMA));
        compra(LocalDate.of(2026,8,2),"Empate",StatusCompra.ATIVA,item(feijao,"1","4",UnidadeMedida.QUILOGRAMA));
        compra(LocalDate.of(2026,8,3),"Rio Verde",StatusCompra.ATIVA,item(feijao,"1000","5",UnidadeMedida.GRAMA));
        compra(LocalDate.of(2026,8,6),"Mista",StatusCompra.ATIVA,item(feijao,"2","12",UnidadeMedida.QUILOGRAMA),item(refrigerante,"3","15",UnidadeMedida.UNIDADE));
        compra(LocalDate.of(2026,8,7),"Ignorada",StatusCompra.ESTORNADA,item(feijao,"1","100",UnidadeMedida.QUILOGRAMA));
        em.flush();

        List<CompraRepository.AnaliseFornecedor> todas=repository.analisarPrecos("","","",null,null);
        var rio=todas.stream().filter(x->x.getItemNome().equals("Feijão")&&x.getFornecedor().equals("Rio Verde")&&x.getUnidade().equals("QUILOGRAMA")).findFirst().orElseThrow();
        assertThat(rio.getMenorPreco()).isEqualByComparingTo("4.000000");
        assertThat(rio.getMaiorPreco()).isEqualByComparingTo("5.000000");
        assertThat(rio.getUltimoPreco()).isEqualByComparingTo("5.000000");
        assertThat(rio.getUltimaCompra()).isEqualTo(LocalDate.of(2026,8,5));
        assertThat(rio.getValorPago()).isEqualByComparingTo("140.00");
        assertThat(rio.getQuantidadeTotal()).isEqualByComparingTo("30.000");
        assertThat(todas).extracting(CompraRepository.AnaliseFornecedor::getUnidade).contains("QUILOGRAMA","GRAMA","UNIDADE");
        assertThat(todas).extracting(CompraRepository.AnaliseFornecedor::getTipoItem).contains("INSUMO","PRODUTO_REVENDA");
        assertThat(repository.analisarPrecos("Feij","Rio","INSUMO",LocalDate.of(2026,8,1),LocalDate.of(2026,8,5)))
                .hasSize(2).allMatch(x->x.getFornecedor().equals("Rio Verde")&&x.getTipoItem().equals("INSUMO"));
        assertThat(repository.analisarPrecos("inexistente","","",null,null)).isEmpty();

        var historico=repository.buscarHistoricoPrecos("INSUMO",feijao.getId(),"QUILOGRAMA","",null,null);
        assertThat(historico).extracting(CompraRepository.HistoricoPreco::getDataCompra)
                .containsExactly(LocalDate.of(2026,8,6),LocalDate.of(2026,8,5),LocalDate.of(2026,8,4),LocalDate.of(2026,8,2),LocalDate.of(2026,8,1));
    }

    @Test void analiseEhExecutadaEmConsultaUnicaSemNMaisUm() {
        compra(LocalDate.of(2026,8,1),"Rio Verde",StatusCompra.ATIVA,item(feijao,"10","40",UnidadeMedida.QUILOGRAMA));
        compra(LocalDate.of(2026,8,2),"Mista",StatusCompra.ATIVA,item(feijao,"2","12",UnidadeMedida.QUILOGRAMA),item(refrigerante,"3","15",UnidadeMedida.UNIDADE));
        em.flush();em.clear();
        var estatisticas=em.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();estatisticas.clear();
        var resultado=repository.analisarPrecos("","","",null,null);
        assertThat(resultado).hasSize(3);
        assertThat(estatisticas.getPrepareStatementCount()).isEqualTo(1);
    }

    private ItemCompra item(Insumo insumo,String quantidade,String valor,UnidadeMedida unidade){return ItemCompra.builder().insumo(insumo).nomeHistorico(insumo.getNome()).unidadeHistorica(unidade).quantidade(new BigDecimal(quantidade)).valorTotalItem(new BigDecimal(valor)).custoUnitario(new BigDecimal(valor).divide(new BigDecimal(quantidade),6,java.math.RoundingMode.HALF_UP)).build();}
    private ItemCompra item(Produto produto,String quantidade,String valor,UnidadeMedida unidade){return ItemCompra.builder().produto(produto).nomeHistorico(produto.getNome()).unidadeHistorica(unidade).quantidade(new BigDecimal(quantidade)).valorTotalItem(new BigDecimal(valor)).custoUnitario(new BigDecimal(valor).divide(new BigDecimal(quantidade),6,java.math.RoundingMode.HALF_UP)).build();}
    private void compra(LocalDate data,String fornecedor,StatusCompra status,ItemCompra...itens){Compra c=Compra.builder().dataCompra(data).fornecedor(fornecedor).status(status).build();for(var i:itens)c.adicionarItem(i);em.persist(c);}
}
