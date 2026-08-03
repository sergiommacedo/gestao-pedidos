package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.produto.*;
import br.com.sergio.gestaopedidos.entity.Produto;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.mapper.ProdutoMapper;
import br.com.sergio.gestaopedidos.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProdutoServiceTest {
    @Test void deveAplicarDefaultsNoCadastroLegado() {
        Fake f=new Fake();f.entidade=produto(1,"Frango",null,true,null);var salvo=f.service().salvar(request(null,null));
        assertThat(salvo.tipoProduto()).isEqualTo(TipoProduto.PRODUZIDO);assertThat(salvo.vendavel()).isTrue();
    }
    @Test void deveBuscarSomenteAtivosEVendaveisIncluindoProduzidoERevenda() {
        Fake f=new Fake();f.busca=List.of(produto(1,"Feijoada",TipoProduto.PRODUZIDO,true,true),produto(2,"Água",TipoProduto.REVENDA,true,true));
        assertThat(f.service().buscarAtivosEVendaveisPorNome("")).extracting(ProdutoResponse::tipoProduto).containsExactly(TipoProduto.PRODUZIDO,TipoProduto.REVENDA);
        assertThat(f.metodoBusca).isEqualTo("findTop20ByAtivoTrueAndVendavelTrueAndNomeContainingIgnoreCaseOrderByNomeAsc");
    }
    @Test void deveAtualizarTipoEVendavel() {
        Fake f=new Fake();f.entidade=produto(1,"Água",TipoProduto.PRODUZIDO,true,true);var r=f.service().atualizar(1L,request(TipoProduto.REVENDA,false));
        assertThat(r.tipoProduto()).isEqualTo(TipoProduto.REVENDA);assertThat(r.vendavel()).isFalse();
    }
    private ProdutoRequest request(TipoProduto t,Boolean v){return ProdutoRequest.builder().nome("Água").preco(BigDecimal.ONE).ativo(true).unidadeVenda(UnidadeVenda.UNIDADE).permiteAcompanhamento(false).tipoProduto(t).vendavel(v).build();}
    private Produto produto(long id,String n,TipoProduto t,Boolean a,Boolean v){return Produto.builder().id(id).nome(n).preco(BigDecimal.ONE).ativo(a).vendavel(v).tipoProduto(t).unidadeVenda(UnidadeVenda.UNIDADE).permiteAcompanhamento(false).build();}
    private ProdutoResponse resposta(Produto p){return ProdutoResponse.builder().id(p.getId()).nome(p.getNome()).preco(p.getPreco()).ativo(p.getAtivo()).vendavel(p.getVendavel()).tipoProduto(p.getTipoProduto()).unidadeVenda(p.getUnidadeVenda()).permiteAcompanhamento(p.getPermiteAcompanhamento()).build();}
    private class Fake implements InvocationHandler {Produto entidade;List<Produto> busca=List.of();String metodoBusca;
        ProdutoService service(){ProdutoRepository r=(ProdutoRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ProdutoRepository.class},this);ProdutoMapper m=(ProdutoMapper)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ProdutoMapper.class},this);return new ProdutoService(r,m);}
        public Object invoke(Object p,Method m,Object[] a){return switch(m.getName()){case"existsByNomeIgnoreCase"->false;case"toEntity"->entidade;case"save"->a[0];case"findById"->Optional.ofNullable(entidade);case"toResponse"->resposta((Produto)a[0]);case"findTop20ByAtivoTrueAndVendavelTrueAndNomeContainingIgnoreCaseOrderByNomeAsc"->{metodoBusca=m.getName();yield busca;}default->null;};}}
}
