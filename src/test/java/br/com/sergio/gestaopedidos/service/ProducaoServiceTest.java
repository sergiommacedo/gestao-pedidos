package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.producao.*;
import br.com.sergio.gestaopedidos.entity.Producao;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ProducaoServiceTest {
    private static final LocalDate DATA = LocalDate.of(2026, 8, 8);

    @Test void deveCalcularComprasSemSobraEValoresNulos() {
        Fake f = new Fake();
        var resposta = f.service().salvar(request(DATA, "0", "1050", "0", null, "30", "20"));
        assertThat(resposta.recursosDisponiveis()).isEqualByComparingTo("1050.00");
        assertThat(resposta.custoMateriaisConsumidos()).isEqualByComparingTo("1050.00");
        assertThat(resposta.valorEmbalagens()).isEqualByComparingTo("0.00");
        assertThat(resposta.totalGasto()).isEqualByComparingTo("1100.00");
    }

    @Test void deveCalcularCenariosComSobraESaldoAnterior() {
        Fake f = new Fake();
        var primeiro = f.service().salvar(request(DATA, "0", "1050", "350", "100", "30", "20"));
        assertThat(primeiro.custoMateriaisConsumidos()).isEqualByComparingTo("700.00");
        assertThat(primeiro.totalGasto()).isEqualByComparingTo("850.00");
        f.entidade = null;
        var segundo = f.service().salvar(request(DATA.plusDays(7), "350", "500", "150", "80", "30", "20"));
        assertThat(segundo.custoMateriaisConsumidos()).isEqualByComparingTo("700.00");
        assertThat(segundo.totalGasto()).isEqualByComparingTo("830.00");
    }

    @Test void deveAceitarSaldoFinalIgualAosRecursos() {
        var resposta = new Fake().service().salvar(request(DATA, "100", "200", "300", "10", "0", "0"));
        assertThat(resposta.custoMateriaisConsumidos()).isZero();
        assertThat(resposta.totalGasto()).isEqualByComparingTo("10.00");
    }

    @Test void deveRejeitarSaldoFinalSuperiorEAsegurarCustoNuncaNegativo() {
        Fake f = new Fake();
        assertThatThrownBy(() -> f.service().salvar(request(DATA, "100", "200", "400", "0", "0", "0")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O saldo final não pode ser maior que o saldo inicial somado às compras.");
        assertThat(f.entidade).isNull();
    }

    @Test void deveRejeitarCadaValorNegativo() {
        Fake f = new Fake();
        assertThatThrownBy(() -> f.service().salvar(request(DATA, "-1", "0", "0", "0", "0", "0"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> f.service().salvar(request(DATA, "0", "-1", "0", "0", "0", "0"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> f.service().salvar(request(DATA, "0", "0", "-1", "0", "0", "0"))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> f.service().salvar(request(DATA, "0", "0", "0", "-1", "0", "0"))).isInstanceOf(BusinessException.class);
    }

    @Test void devePreservarCustoDoRegistroLegado() {
        Fake f = new Fake();
        f.entidade = legado(1L, "600", "50", "20", "10");
        var resposta = f.service().buscarPorId(1L);
        assertThat(resposta.saldoInicialMateriais()).isZero();
        assertThat(resposta.valorComprasMateriais()).isEqualByComparingTo("600.00");
        assertThat(resposta.saldoFinalMateriais()).isZero();
        assertThat(resposta.custoMateriaisConsumidos()).isEqualByComparingTo("600.00");
        assertThat(resposta.totalGasto()).isEqualByComparingTo("680.00");
    }

    @Test void registroJaMigradoNaoUsaNemSobrescreveValorLegado() {
        Fake f = new Fake();
        f.entidade = entidade(1L, DATA, "350", "500", "150", "80", "30", "20");
        f.entidade.setValorIngredientes(bd("9999"));
        var resposta = f.service().buscarPorId(1L);
        assertThat(resposta.custoMateriaisConsumidos()).isEqualByComparingTo("700.00");
        assertThat(f.entidade.getValorIngredientes()).isEqualByComparingTo("9999");
    }

    @Test void deveSugerirSaldoFinalAnteriorSemCriarVinculo() {
        Fake f = new Fake();
        f.anterior = entidade(1L, DATA.minusDays(7), "0", "500", "175", "0", "0", "0");
        assertThat(f.service().sugerirSaldoInicial(DATA)).isEqualByComparingTo("175.00");
        assertThat(f.dataConsultadaAnterior).isEqualTo(DATA);
    }

    @Test void deveSugerirProducaoMaisRecenteQuandoDataNaoInformada() {
        Fake f = new Fake();
        f.maisRecente = entidade(1L, DATA, "0", "500", "125", "0", "0", "0");
        assertThat(f.service().sugerirSaldoInicial(null)).isEqualByComparingTo("125.00");
    }

    @Test void edicaoPreservaSaldoSalvoEPermiteManterData() {
        Fake f = new Fake();
        f.entidade = entidade(1L, DATA, "350", "500", "150", "0", "0", "0");
        f.anterior = entidade(2L, DATA.minusDays(7), "0", "100", "90", "0", "0", "0");
        var resposta = f.service().atualizar(1L, request(DATA, "350", "500", "150", "10", "0", "0"));
        assertThat(resposta.saldoInicialMateriais()).isEqualByComparingTo("350.00");
        assertThat(f.consultouAnterior).isFalse();
    }

    @Test void deveCalcularResultadoPositivoNegativoZeradoMargemEFaturamentoZero() {
        Fake f = new Fake();
        f.entidade = entidade(1L, DATA, "0", "150", "0", "0", "0", "0");
        f.financeiro = financeiro(DATA, 3, "270", "30");
        var positivo = f.service().buscarResumoPorId(1L);
        assertThat(positivo.resultadoBrutoEstimado()).isEqualByComparingTo("150.00");
        assertThat(positivo.margemBrutaEstimada()).isEqualByComparingTo("50.00");
        f.entidade.setValorComprasMateriais(bd("350"));
        assertThat(f.service().buscarResumoPorId(1L).resultadoBrutoEstimado()).isEqualByComparingTo("-50.00");
        f.entidade.setValorComprasMateriais(bd("300"));
        assertThat(f.service().buscarResumoPorId(1L).resultadoBrutoEstimado()).isZero();
        f.financeiro = null;
        var semFaturamento = f.service().buscarResumoPorId(1L);
        assertThat(semFaturamento.margemBrutaEstimada()).isZero();
        assertThat(semFaturamento.resultadoBrutoEstimado()).isEqualByComparingTo("-300.00");
    }

    @Test void devePaginarFiltrarAgregarPedidosValidosEExcluirSemAlterarPedidos() {
        Fake f = new Fake();
        f.entidade = entidade(1L, DATA, "0", "4", "0", "0", "0", "0");
        f.financeiro = financeiro(DATA, 1, "10", "2"); f.total = 15;
        var page = PageRequest.of(1, 10, Sort.by("dataProducao"));
        var resposta = f.service().listar(DATA.minusDays(7), DATA, page);
        assertThat(resposta.getNumber()).isEqualTo(1);
        assertThat(resposta.getTotalElements()).isEqualTo(11);
        assertThat(resposta.getContent().getFirst().pedidosValidos()).isEqualTo(1);
        assertThat(resposta.getContent().getFirst().faturamentoTotal()).isEqualByComparingTo("12.00");
        f.service().excluir(1L);
        assertThat(f.excluiuProducao).isTrue();
        assertThat(f.alterouPedido).isFalse();
    }

    private ProducaoRequest request(LocalDate data, String inicial, String compras, String saldoFinal,
                                     String embalagens, String gas, String outros) {
        return ProducaoRequest.builder().dataProducao(data).saldoInicialMateriais(bdOuNulo(inicial))
                .valorComprasMateriais(bdOuNulo(compras)).saldoFinalMateriais(bdOuNulo(saldoFinal))
                .valorEmbalagens(bdOuNulo(embalagens)).valorGasEnergia(bdOuNulo(gas))
                .valorOutros(bdOuNulo(outros)).observacao(" teste ").build();
    }

    private Producao entidade(Long id, LocalDate data, String inicial, String compras, String saldoFinal,
                              String embalagens, String gas, String outros) {
        return Producao.builder().id(id).dataProducao(data).valorIngredientes(BigDecimal.ZERO)
                .saldoInicialMateriais(bd(inicial)).valorComprasMateriais(bd(compras)).saldoFinalMateriais(bd(saldoFinal))
                .valorEmbalagens(bd(embalagens)).valorGasEnergia(bd(gas)).valorOutros(bd(outros)).build();
    }

    private Producao legado(Long id, String ingredientes, String embalagens, String gas, String outros) {
        return Producao.builder().id(id).dataProducao(DATA).valorIngredientes(bd(ingredientes))
                .valorEmbalagens(bd(embalagens)).valorGasEnergia(bd(gas)).valorOutros(bd(outros)).build();
    }

    private PedidoRepository.ResumoFinanceiroProducao financeiro(LocalDate data, long pedidos, String produtos, String taxas) {
        Map<String,Object> valores = Map.of("getDataProducao", data, "getPedidosValidos", pedidos,
                "getFaturamentoProdutos", bd(produtos), "getTaxasEntrega", bd(taxas),
                "getFaturamentoTotal", bd(produtos).add(bd(taxas)));
        return (PedidoRepository.ResumoFinanceiroProducao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{PedidoRepository.ResumoFinanceiroProducao.class}, (proxy, method, args) -> valores.get(method.getName()));
    }

    private static BigDecimal bd(String valor) { return new BigDecimal(valor); }
    private static BigDecimal bdOuNulo(String valor) { return valor == null ? null : bd(valor); }

    private class Fake implements InvocationHandler {
        Producao entidade, anterior, maisRecente;
        PedidoRepository.ResumoFinanceiroProducao financeiro;
        boolean duplicada, duplicadaAtualizacao, excluiuProducao, alterouPedido, consultouAnterior;
        LocalDate dataConsultadaAnterior;
        long total = 1;
        ProducaoRepository producoes;

        ProducaoService service() {
            ClassLoader loader = getClass().getClassLoader();
            producoes = (ProducaoRepository) Proxy.newProxyInstance(loader, new Class[]{ProducaoRepository.class}, this);
            PedidoRepository pedidos = (PedidoRepository) Proxy.newProxyInstance(loader, new Class[]{PedidoRepository.class}, this);
            return new ProducaoService(producoes, pedidos);
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsByDataProducao" -> duplicada;
                case "existsByDataProducaoAndIdNot" -> duplicadaAtualizacao;
                case "saveAndFlush" -> { entidade = (Producao) args[0]; if (entidade.getId() == null) entidade.setId(1L); yield entidade; }
                case "findById" -> Optional.ofNullable(entidade);
                case "findFirstByOrderByDataProducaoDesc" -> Optional.ofNullable(maisRecente);
                case "findFirstByDataProducaoLessThanOrderByDataProducaoDesc" -> { consultouAnterior = true; dataConsultadaAnterior = (LocalDate) args[0]; yield Optional.ofNullable(anterior); }
                case "delete" -> { if (proxy == producoes) excluiuProducao = true; else alterouPedido = true; yield null; }
                case "resumirFinanceiroProducao" -> Optional.ofNullable(financeiro);
                case "buscarPorPeriodo" -> new PageImpl<>(entidade == null ? List.of() : List.of(entidade), (Pageable) args[2], total);
                case "resumirFinanceiroProducoes" -> financeiro == null ? List.of() : List.of(financeiro);
                default -> null;
            };
        }
    }
}
