package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.*;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class RelatorioFinanceiroServiceTest {
    private static final LocalDate INICIO = LocalDate.of(2026, 8, 1), FIM = LocalDate.of(2026, 8, 31);

    @Test
    void deveMontarFaturamentoProdutosTaxasTicketCanceladosMelhorDiaEAgrupamentos() {
        var dia = new RelatorioFinanceiroDiaResponse(FIM, 3L, 1L, bd("270"), bd("30"), bd("300"), BigDecimal.ZERO);
        var pagamento = new RelatorioFinanceiroPagamentoResponse(FormaPagamento.PIX, 2L, bd("200"), bd("66.666"), BigDecimal.ZERO);
        var entrega = new RelatorioFinanceiroEntregaResponse(TipoEntrega.ENTREGA, 2L, bd("180"), bd("20"), bd("200"), bd("66.666"));
        Fake fake = new Fake(new PageImpl<>(List.of(dia)), indicador(4, 3, 1, "270", "30", "300", "80"),
                List.of(pagamento), List.of(entrega), melhor(FIM, "300"));

        var resultado = fake.service().buscar(filtro(null, null, null, ""), PageRequest.of(0, 10));

        assertThat(resultado.indicadores().faturamentoProdutos()).isEqualByComparingTo("270");
        assertThat(resultado.indicadores().taxasEntrega()).isEqualByComparingTo("30");
        assertThat(resultado.indicadores().faturamentoBruto()).isEqualByComparingTo("300");
        assertThat(resultado.indicadores().ticketMedio()).isEqualByComparingTo("100.00");
        assertThat(resultado.indicadores().valorCancelado()).isEqualByComparingTo("80");
        assertThat(resultado.indicadores().melhorDia()).isEqualTo(FIM);
        assertThat(resultado.dias().getContent().getFirst().ticketMedio()).isEqualByComparingTo("100.00");
        assertThat(resultado.pagamentos().getFirst().participacaoPercentual()).isEqualByComparingTo("66.67");
        assertThat(resultado.pagamentos().getFirst().ticketMedio()).isEqualByComparingTo("100.00");
        assertThat(resultado.entregas().getFirst().faturamentoTotal()).isEqualByComparingTo("200");
    }

    @Test
    void deveManterReceitaZeroQuandoFiltroForCanceladoEEncaminharTodosOsFiltros() {
        Fake fake = new Fake(Page.empty(), indicador(2, 0, 2, "0", "0", "0", "150"), List.of(), List.of(), null);
        var resultado = fake.service().buscar(
                filtro(FormaPagamento.PIX, TipoEntrega.RETIRADA, StatusPedido.CANCELADO, "11999990000"),
                PageRequest.of(2, 20)
        );
        assertThat(resultado.indicadores().faturamentoBruto()).isZero();
        assertThat(resultado.indicadores().ticketMedio()).isEqualByComparingTo("0.00");
        assertThat(resultado.indicadores().valorCancelado()).isEqualByComparingTo("150");
        Object[] args = fake.argsDias.get();
        assertThat(args[2]).isEqualTo(FormaPagamento.PIX); assertThat(args[3]).isEqualTo(TipoEntrega.RETIRADA);
        assertThat(args[4]).isEqualTo(StatusPedido.CANCELADO); assertThat(args[5]).isEqualTo("11999990000");
        assertThat(args[6]).isEqualTo(StatusPedido.CANCELADO);
        assertThat(((Pageable) args[7]).getPageNumber()).isEqualTo(2);
    }

    @Test
    void deveRetornarVazioSemMelhorDia() {
        var resultado = vazio().service().buscar(filtro(null, null, null, ""), PageRequest.of(0, 10));
        assertThat(resultado.dias()).isEmpty(); assertThat(resultado.pagamentos()).isEmpty();
        assertThat(resultado.entregas()).isEmpty(); assertThat(resultado.indicadores().melhorDia()).isNull();
        assertThat(resultado.indicadores().faturamentoBruto()).isZero();
    }

    @Test
    void deveValidarPeriodoESeuLimite() {
        var service = vazio().service();
        assertThatThrownBy(() -> service.buscar(
                new RelatorioFinanceiroService.FiltroRelatorioFinanceiro(FIM, INICIO, null, null, null, ""),
                PageRequest.of(0, 10))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.buscar(
                new RelatorioFinanceiroService.FiltroRelatorioFinanceiro(
                        LocalDate.of(1990, 1, 1), LocalDate.of(2020, 1, 1), null, null, null, ""),
                PageRequest.of(0, 10))).isInstanceOf(BusinessException.class).hasMessageContaining("10.000 dias");
    }

    @Test
    void deveLerTodosOsDiasEmLotesParaImpressaoEExcel() {
        var dia = new RelatorioFinanceiroDiaResponse(INICIO, 1L, 0L, bd("90"), bd("10"), bd("100"), bd("100"));
        Fake fake = vazio(); fake.lotes = List.of(new SliceImpl<>(List.of(dia), PageRequest.of(0, 500), true), new SliceImpl<>(List.of(dia), PageRequest.of(1, 500), false));
        var resultado = fake.service().buscarParaSaida(filtro(null, null, null, ""), Sort.by("dataAgendada"));
        assertThat(resultado.dias()).hasSize(2);
    }

    private RelatorioFinanceiroService.FiltroRelatorioFinanceiro filtro(FormaPagamento p, TipoEntrega e, StatusPedido s, String c) { return new RelatorioFinanceiroService.FiltroRelatorioFinanceiro(INICIO, FIM, p, e, s, c); }
    private BigDecimal bd(String v) { return new BigDecimal(v); }
    private Fake vazio() { return new Fake(Page.empty(), indicador(0,0,0,"0","0","0","0"), List.of(), List.of(), null); }

    private PedidoRepository.IndicadoresRelatorioFinanceiro indicador(long total,long validos,long cancelados,String produtos,String taxas,String bruto,String cancelado) {
        return proxy(PedidoRepository.IndicadoresRelatorioFinanceiro.class, Map.of(
                "getPedidosTotais",total,"getPedidosValidos",validos,"getCancelados",cancelados,
                "getFaturamentoProdutos",bd(produtos),"getTaxasEntrega",bd(taxas),
                "getFaturamentoBruto",bd(bruto),"getValorCancelado",bd(cancelado)));
    }
    private PedidoRepository.MelhorDiaRelatorioFinanceiro melhor(LocalDate data,String valor) { return proxy(PedidoRepository.MelhorDiaRelatorioFinanceiro.class,Map.of("getData",data,"getFaturamento",bd(valor))); }
    private <T>T proxy(Class<T> tipo,Map<String,Object> valores){return tipo.cast(Proxy.newProxyInstance(tipo.getClassLoader(),new Class[]{tipo},(p,m,a)->valores.get(m.getName())));}

    private class Fake implements InvocationHandler {
        Page<RelatorioFinanceiroDiaResponse> pagina; PedidoRepository.IndicadoresRelatorioFinanceiro indicador;
        List<RelatorioFinanceiroPagamentoResponse> pagamentos; List<RelatorioFinanceiroEntregaResponse> entregas;
        PedidoRepository.MelhorDiaRelatorioFinanceiro melhor; List<Slice<RelatorioFinanceiroDiaResponse>> lotes=List.of(); int lote;
        AtomicReference<Object[]> argsDias=new AtomicReference<>();
        Fake(Page<RelatorioFinanceiroDiaResponse> p,PedidoRepository.IndicadoresRelatorioFinanceiro i,List<RelatorioFinanceiroPagamentoResponse> pagamentos,List<RelatorioFinanceiroEntregaResponse> entregas,PedidoRepository.MelhorDiaRelatorioFinanceiro melhor){this.pagina=p;this.indicador=i;this.pagamentos=pagamentos;this.entregas=entregas;this.melhor=melhor;}
        RelatorioFinanceiroService service(){return new RelatorioFinanceiroService((PedidoRepository)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PedidoRepository.class},this));}
        public Object invoke(Object p,Method m,Object[] a){return switch(m.getName()){
            case"buscarDiasRelatorioFinanceiro"->{argsDias.set(a);yield pagina;}
            case"buscarIndicadoresRelatorioFinanceiro"->indicador;
            case"buscarPagamentosRelatorioFinanceiro"->pagamentos;
            case"buscarEntregasRelatorioFinanceiro"->entregas;
            case"buscarMelhorDiaRelatorioFinanceiro"->melhor==null?List.of():List.of(melhor);
            case"buscarLoteDiasRelatorioFinanceiro"->lotes.isEmpty()?new SliceImpl<>(List.of(),(Pageable)a[7],false):lotes.get(lote++);
            default->null;};}
    }
}
