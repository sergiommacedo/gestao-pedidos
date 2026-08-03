package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioProducaoLinhaResponse;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.ItemPedidoRepository;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class RelatorioProducaoServiceTest {
    private static final LocalDate INICIO=LocalDate.of(2026,8,1), FIM=LocalDate.of(2026,8,31);

    @Test void deveMontarIndicadoresRankingMediaParticipacaoUnidadeQuiloTaxasETotal() {
        var linhas=List.of(linha(1,"Frango",UnidadeVenda.UNIDADE,"3","150","75","40",2),
                linha(2,"Joelho",UnidadeVenda.QUILOGRAMA,"4.250","225","112.50","60",1));
        Fake fake=new Fake(new PageImpl<>(linhas), indicador(2,"3","4.250","375"), lider("Joelho","225"), new BigDecimal("35"));
        var r=fake.service().buscar(filtro("",null,null,null),PageRequest.of(0,10));
        assertThat(r.linhas()).hasSize(2); assertThat(r.linhas().getContent().get(1).quantidadeTotal()).isEqualByComparingTo("4.250");
        assertThat(r.linhas().getContent().get(0).mediaPorPedido()).isEqualByComparingTo("75.00");
        assertThat(r.linhas().getContent().get(1).participacaoPercentual()).isEqualByComparingTo("60.00");
        assertThat(r.linhas().getContent().get(1).posicao()).isEqualTo(1);
        assertThat(r.indicadores().totalUnidades()).isEqualByComparingTo("3");
        assertThat(r.indicadores().totalQuilogramas()).isEqualByComparingTo("4.250");
        assertThat(r.indicadores().taxasEntrega()).isEqualByComparingTo("35");
        assertThat(r.indicadores().totalGeral()).isEqualByComparingTo("410");
        assertThat(r.indicadores().produtoLiderNome()).isEqualTo("Joelho");
    }

    @Test void deveEncaminharTodosOsFiltrosEExcluirCancelado() {
        Fake fake=fakeVazio();
        fake.service().buscar(filtro("Frango",UnidadeVenda.UNIDADE,TipoEntrega.ENTREGA,FormaPagamento.PIX),PageRequest.of(0,20));
        Object[] a=fake.argsPagina.get(); assertThat(a[2]).isEqualTo("Frango"); assertThat(a[3]).isEqualTo(UnidadeVenda.UNIDADE);
        assertThat(a[4]).isEqualTo(TipoEntrega.ENTREGA); assertThat(a[5]).isEqualTo(FormaPagamento.PIX);
        assertThat(a[6]).isEqualTo(StatusPedido.CANCELADO);
    }

    @Test void deveRetornarVazioComLiderNenhum() {
        var r=fakeVazio().service().buscar(filtro("",null,null,null),PageRequest.of(0,10));
        assertThat(r.linhas()).isEmpty(); assertThat(r.indicadores().produtosDistintos()).isZero();
        assertThat(r.indicadores().produtoLiderNome()).isEqualTo("Nenhum");
    }

    @Test void deveRejeitarPeriodoInvalido() {
        assertThatThrownBy(()->fakeVazio().service().buscar(
                new RelatorioProducaoService.FiltroRelatorioProducao(FIM,INICIO,"",null,null,null),PageRequest.of(0,10)))
                .isInstanceOf(BusinessException.class);
    }

    @Test void devePreservarPaginacao() {
        Fake fake=new Fake(new PageImpl<>(List.of(),PageRequest.of(2,20),45),indicador(0,"0","0","0"),null,BigDecimal.ZERO);
        var p=fake.service().buscar(filtro("",null,null,null),PageRequest.of(2,20)).linhas();
        assertThat(p.getNumber()).isEqualTo(2); assertThat(p.getTotalElements()).isEqualTo(45);
    }

    @Test void deveAplicarLimiteDeDezMilNaImpressaoEExcel() {
        Fake fake=fakeVazio(); fake.loteCheio=true;
        assertThatThrownBy(()->fake.service().buscarParaSaida(filtro("",null,null,null),Sort.by("produto.nome")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("10.000 produtos");
    }

    private RelatorioProducaoService.FiltroRelatorioProducao filtro(String p,UnidadeVenda u,TipoEntrega t,FormaPagamento f){return new RelatorioProducaoService.FiltroRelatorioProducao(INICIO,FIM,p,u,t,f);}
    private Fake fakeVazio(){return new Fake(Page.empty(),indicador(0,"0","0","0"),null,BigDecimal.ZERO);}
    private RelatorioProducaoLinhaResponse linha(long id,String n,UnidadeVenda u,String q,String fat,String media,String part,long pos){return new RelatorioProducaoLinhaResponse(id,n,u,new BigDecimal(q),2,new BigDecimal(fat),new BigDecimal(media),new BigDecimal(part),pos);}
    private ItemPedidoRepository.IndicadoresRelatorioProducao indicador(long p,String u,String kg,String f){return proxy(ItemPedidoRepository.IndicadoresRelatorioProducao.class,Map.of("getProdutosDistintos",p,"getTotalUnidades",new BigDecimal(u),"getTotalQuilogramas",new BigDecimal(kg),"getFaturamentoProdutos",new BigDecimal(f)));}
    private ItemPedidoRepository.ProdutoLiderRelatorioProducao lider(String n,String f){return proxy(ItemPedidoRepository.ProdutoLiderRelatorioProducao.class,Map.of("getProdutoNome",n,"getFaturamentoTotal",new BigDecimal(f)));}
    private <T>T proxy(Class<T> c,Map<String,Object> m){return c.cast(Proxy.newProxyInstance(c.getClassLoader(),new Class[]{c},(p,x,a)->m.get(x.getName())));}

    private class Fake implements InvocationHandler {
        Page<RelatorioProducaoLinhaResponse> page; ItemPedidoRepository.IndicadoresRelatorioProducao ind; ItemPedidoRepository.ProdutoLiderRelatorioProducao lider; BigDecimal taxas; boolean loteCheio; AtomicReference<Object[]> argsPagina=new AtomicReference<>();
        Fake(Page<RelatorioProducaoLinhaResponse> p,ItemPedidoRepository.IndicadoresRelatorioProducao i,ItemPedidoRepository.ProdutoLiderRelatorioProducao l,BigDecimal t){page=p;ind=i;lider=l;taxas=t;}
        RelatorioProducaoService service(){ItemPedidoRepository ir=(ItemPedidoRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ItemPedidoRepository.class},this);PedidoRepository pr=(PedidoRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PedidoRepository.class},(p,m,a)->m.getName().equals("somarTaxasRelatorioProducao")?taxas:null);return new RelatorioProducaoService(ir,pr);}
        public Object invoke(Object p,Method m,Object[] a){return switch(m.getName()){case"buscarRelatorioProducao"->{argsPagina.set(a);yield page;}case"buscarIndicadoresRelatorioProducao"->ind;case"buscarProdutoLiderRelatorioProducao"->lider==null?List.of():List.of(lider);case"buscarLoteRelatorioProducao"->{List<RelatorioProducaoLinhaResponse> c=loteCheio?Collections.nCopies(500,linha(1,"Produto",UnidadeVenda.UNIDADE,"1","1","1","1",1)):List.of();yield new SliceImpl<>(c,(Pageable)a[7],loteCheio);}default->null;};}
    }
}
