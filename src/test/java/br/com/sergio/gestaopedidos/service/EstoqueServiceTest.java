package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.estoque.*;
import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class EstoqueServiceTest {
    @Test void entradasCalculamCustoMedioPonderadoESaidaMantemCusto() {
        Fake f=new Fake(UnidadeMedida.QUILOGRAMA,true); EstoqueService s=f.service();
        s.registrarEntradaManual(entrada("10","100")); s.registrarEntradaManual(entrada("10","200"));
        assertThat(f.saldo.getQuantidadeAtual()).isEqualByComparingTo("20.000");
        assertThat(f.saldo.getCustoMedioAtual()).isEqualByComparingTo("15.000000");
        s.registrarSaidaManual(saida("5",TipoMovimentacaoEstoque.SAIDA_CONSUMO_MANUAL));
        assertThat(f.saldo.getQuantidadeAtual()).isEqualByComparingTo("15.000");
        assertThat(f.saldo.getValorTotalEstoque()).isEqualByComparingTo("225.00");
        assertThat(f.saldo.getCustoMedioAtual()).isEqualByComparingTo("15.000000");
    }
    @Test void rejeitaSaldoInsuficienteInsumoInativoEUnidadeFracionada() {
        Fake f=new Fake(UnidadeMedida.UNIDADE,true);EstoqueService s=f.service();s.registrarEntradaManual(entrada("2","20"));
        assertThatThrownBy(()->s.registrarSaidaManual(saida("3",TipoMovimentacaoEstoque.SAIDA_PERDA))).hasMessageContaining("Estoque insuficiente");
        assertThatThrownBy(()->s.registrarEntradaManual(entrada("1.5","10"))).hasMessageContaining("deve ser inteira");
        f.insumo.setAtivo(false);assertThatThrownBy(()->s.registrarEntradaManual(entrada("1","10"))).hasMessageContaining("não está disponível");
    }
    @Test void compraGeraUmaEntradaEEstornoReverteSemApagarHistorico() {
        Fake f=new Fake(UnidadeMedida.QUILOGRAMA,true);EstoqueService s=f.service();CompraInsumo c=f.compra("4","40");
        s.processarCompra(c);s.processarCompra(c);
        assertThat(f.movimentos).hasSize(1);assertThat(f.saldo.getQuantidadeAtual()).isEqualByComparingTo("4.000");
        s.estornarCompra(c);assertThat(c.getStatus()).isEqualTo(StatusCompraInsumo.ESTORNADA);assertThat(f.movimentos).hasSize(2);assertThat(f.saldo.getQuantidadeAtual()).isZero();
        assertThatThrownBy(()->s.estornarCompra(c)).hasMessage("Esta compra já foi estornada.");
    }
    private EntradaEstoqueRequest entrada(String q,String v){return EntradaEstoqueRequest.builder().insumoId(1L).quantidade(new BigDecimal(q)).valorTotal(new BigDecimal(v)).dataMovimentacao(LocalDateTime.now()).build();}
    private SaidaEstoqueRequest saida(String q,TipoMovimentacaoEstoque t){return SaidaEstoqueRequest.builder().insumoId(1L).quantidade(new BigDecimal(q)).tipo(t).dataMovimentacao(LocalDateTime.now()).build();}

    static class Fake {
        final Insumo insumo; EstoqueInsumo saldo; final List<MovimentacaoEstoque> movimentos=new ArrayList<>(); long movimentoId=1;
        Fake(UnidadeMedida unidade,boolean ativo){insumo=Insumo.builder().id(1L).nome("Feijão").unidadeMedida(unidade).ativo(ativo).estoqueMinimo(BigDecimal.ZERO).build();}
        EstoqueService service(){
            EstoqueInsumoRepository er=(EstoqueInsumoRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{EstoqueInsumoRepository.class},(p,m,a)->switch(m.getName()){
                case "bloquearInsumo","buscarInsumo"->Optional.of(insumo);case "findByInsumoId","buscarSaldo"->Optional.ofNullable(saldo);case "save"->{saldo=(EstoqueInsumo)a[0];yield saldo;}default->zero(m.getReturnType());});
            MovimentacaoEstoqueRepository mr=(MovimentacaoEstoqueRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{MovimentacaoEstoqueRepository.class},(p,m,a)->switch(m.getName()){
                case "existsByItemCompraInsumoIdAndTipo"->movimentos.stream().anyMatch(x->x.getItemCompraInsumo()!=null&&Objects.equals(x.getItemCompraInsumo().getId(),a[0])&&x.getTipo()==a[1]);
                case "save"->{MovimentacaoEstoque x=(MovimentacaoEstoque)a[0];x.setId(movimentoId++);movimentos.add(x);yield x;}
                case "findByItemCompraInsumoCompraIdAndTipoOrderByIdAsc"->movimentos.stream().filter(x->x.getItemCompraInsumo()!=null&&Objects.equals(x.getItemCompraInsumo().getCompra().getId(),a[0])&&x.getTipo()==a[1]).toList();default->zero(m.getReturnType());});
            ItemCompraInsumoRepository ir=(ItemCompraInsumoRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ItemCompraInsumoRepository.class},(p,m,a)->zero(m.getReturnType()));
            return new EstoqueService(er,mr,ir);
        }
        CompraInsumo compra(String q,String v){CompraInsumo c=CompraInsumo.builder().id(7L).dataCompra(LocalDate.now()).status(StatusCompraInsumo.ATIVA).build();ItemCompraInsumo i=ItemCompraInsumo.builder().id(9L).insumo(insumo).unidadeMedida(insumo.getUnidadeMedida()).quantidade(new BigDecimal(q)).valorTotalItem(new BigDecimal(v)).custoUnitario(new BigDecimal(v).divide(new BigDecimal(q),6,java.math.RoundingMode.HALF_UP)).build();c.adicionarItem(i);return c;}
        static Object zero(Class<?> t){if(t==boolean.class)return false;if(t==long.class)return 0L;if(t==int.class)return 0;if(Optional.class.isAssignableFrom(t))return Optional.empty();return null;}
    }
}
