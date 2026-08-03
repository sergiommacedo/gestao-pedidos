package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.producao.*;
import br.com.sergio.gestaopedidos.entity.Producao;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.exception.*;
import br.com.sergio.gestaopedidos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProducaoService {
    private final ProducaoRepository producaoRepository;
    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public Page<ProducaoResumoResponse> listar(LocalDate inicio, LocalDate fim, Pageable pageable) {
        validarPeriodo(inicio, fim);
        Page<Producao> pagina = producaoRepository.buscarPorPeriodo(inicio, fim, pageable);
        List<LocalDate> datas = pagina.getContent().stream().map(Producao::getDataProducao).toList();
        Map<LocalDate, PedidoRepository.ResumoFinanceiroProducao> financeiros = datas.isEmpty()
                ? Map.of()
                : pedidoRepository.resumirFinanceiroProducoes(datas, StatusPedido.CANCELADO).stream()
                    .collect(Collectors.toMap(PedidoRepository.ResumoFinanceiroProducao::getDataProducao, Function.identity()));
        List<ProducaoResumoResponse> respostas = pagina.getContent().stream()
                .map(p -> montarResumo(p, financeiros.get(p.getDataProducao()))).toList();
        return new PageImpl<>(respostas, pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProducaoResumoResponse buscarResumoPorId(Long id) {
        Producao producao = buscarEntidade(id);
        return montarResumo(producao, pedidoRepository.resumirFinanceiroProducao(
                producao.getDataProducao(), StatusPedido.CANCELADO).orElse(null));
    }

    @Transactional(readOnly = true)
    public Optional<ProducaoResumoResponse> buscarPorData(LocalDate data) {
        return producaoRepository.findByDataProducao(data).map(p -> montarResumo(p,
                pedidoRepository.resumirFinanceiroProducao(data, StatusPedido.CANCELADO).orElse(null)));
    }

    @Transactional(readOnly = true)
    public ProducaoResponse buscarPorId(Long id) { return mapear(buscarEntidade(id)); }

    @Transactional(readOnly = true)
    public BigDecimal sugerirSaldoInicial(LocalDate dataReferencia) {
        Optional<Producao> anterior = dataReferencia == null
                ? producaoRepository.findFirstByOrderByDataProducaoDesc()
                : producaoRepository.findFirstByDataProducaoLessThanOrderByDataProducaoDesc(dataReferencia);
        return anterior.map(this::valoresMateriais).map(ValoresMateriais::saldoFinal).orElseGet(this::zero);
    }

    public ProducaoResponse salvar(ProducaoRequest request) {
        if (producaoRepository.existsByDataProducao(request.dataProducao())) duplicada();
        Producao producao = new Producao(); aplicar(producao, request);
        return mapear(salvarSeguro(producao));
    }

    public ProducaoResponse atualizar(Long id, ProducaoRequest request) {
        Producao producao = buscarEntidade(id);
        if (producaoRepository.existsByDataProducaoAndIdNot(request.dataProducao(), id)) duplicada();
        aplicar(producao, request);
        return mapear(salvarSeguro(producao));
    }

    public void excluir(Long id) { producaoRepository.delete(buscarEntidade(id)); }

    private Producao salvarSeguro(Producao producao) {
        try { return producaoRepository.saveAndFlush(producao); }
        catch (DataIntegrityViolationException e) { throw new BusinessException("Já existe uma produção cadastrada para esta data."); }
    }

    private void aplicar(Producao p, ProducaoRequest r) {
        BigDecimal saldoInicial = moeda(r.saldoInicialMateriais());
        BigDecimal compras = moeda(r.valorComprasMateriais());
        BigDecimal saldoFinal = moeda(r.saldoFinalMateriais());
        validarMateriais(saldoInicial, compras, saldoFinal);
        p.setDataProducao(r.dataProducao());
        p.setSaldoInicialMateriais(saldoInicial); p.setValorComprasMateriais(compras);
        p.setSaldoFinalMateriais(saldoFinal);
        if (p.getValorIngredientes() == null) p.setValorIngredientes(zero());
        p.setValorEmbalagens(moeda(r.valorEmbalagens())); p.setValorGasEnergia(moeda(r.valorGasEnergia()));
        p.setValorOutros(moeda(r.valorOutros()));
        validarNaoNegativo(p.getValorEmbalagens(), "Embalagens");
        validarNaoNegativo(p.getValorGasEnergia(), "Gás/Energia");
        validarNaoNegativo(p.getValorOutros(), "Outros");
        p.setObservacao(r.observacao() == null ? null : r.observacao().trim());
    }

    private ProducaoResumoResponse montarResumo(Producao p, PedidoRepository.ResumoFinanceiroProducao f) {
        ProducaoResponse resposta = mapear(p); BigDecimal produtos = f == null ? zero() : moeda(f.getFaturamentoProdutos());
        BigDecimal taxas = f == null ? zero() : moeda(f.getTaxasEntrega());
        BigDecimal faturamento = produtos.add(taxas).setScale(2, RoundingMode.HALF_UP);
        BigDecimal resultado = calcularResultado(faturamento, resposta.totalGasto());
        BigDecimal margem = calcularMargem(resultado, faturamento);
        return ProducaoResumoResponse.builder().producao(resposta).pedidosValidos(f == null || f.getPedidosValidos() == null ? 0 : f.getPedidosValidos())
                .faturamentoProdutos(produtos).taxasEntrega(taxas).faturamentoTotal(faturamento)
                .totalGasto(resposta.totalGasto()).resultadoBrutoEstimado(resultado).margemBrutaEstimada(margem).build();
    }

    private ProducaoResponse mapear(Producao p) {
        ValoresMateriais materiais = valoresMateriais(p);
        BigDecimal recursos = calcularRecursosDisponiveis(materiais.saldoInicial(), materiais.compras());
        BigDecimal consumidos = calcularCustoMateriaisConsumidos(recursos, materiais.saldoFinal());
        BigDecimal embalagens=moeda(p.getValorEmbalagens());
        BigDecimal gas=moeda(p.getValorGasEnergia()), outros=moeda(p.getValorOutros());
        BigDecimal outrosCustos = calcularOutrosCustos(embalagens, gas, outros);
        return ProducaoResponse.builder().id(p.getId()).dataProducao(p.getDataProducao())
                .saldoInicialMateriais(materiais.saldoInicial()).valorComprasMateriais(materiais.compras())
                .saldoFinalMateriais(materiais.saldoFinal()).recursosDisponiveis(recursos)
                .custoMateriaisConsumidos(consumidos).valorEmbalagens(embalagens)
                .valorGasEnergia(gas).valorOutros(outros).outrosCustos(outrosCustos)
                .totalGasto(calcularTotalGasto(consumidos, outrosCustos))
                .observacao(p.getObservacao()).criadoEm(p.getCriadoEm()).atualizadoEm(p.getAtualizadoEm()).build();
    }

    private ValoresMateriais valoresMateriais(Producao p) {
        boolean aindaLegado = p.getSaldoInicialMateriais() == null
                && p.getValorComprasMateriais() == null && p.getSaldoFinalMateriais() == null;
        if (aindaLegado) return new ValoresMateriais(zero(), moeda(p.getValorIngredientes()), zero());
        return new ValoresMateriais(moeda(p.getSaldoInicialMateriais()), moeda(p.getValorComprasMateriais()),
                moeda(p.getSaldoFinalMateriais()));
    }

    private BigDecimal calcularRecursosDisponiveis(BigDecimal saldoInicial, BigDecimal compras) {
        return saldoInicial.add(compras).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularCustoMateriaisConsumidos(BigDecimal recursos, BigDecimal saldoFinal) {
        BigDecimal consumido = recursos.subtract(saldoFinal).setScale(2, RoundingMode.HALF_UP);
        if (consumido.signum() < 0) throw new BusinessException("O custo dos materiais consumidos não pode ser negativo.");
        return consumido;
    }

    private BigDecimal calcularOutrosCustos(BigDecimal embalagens, BigDecimal gas, BigDecimal outros) {
        return embalagens.add(gas).add(outros).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularTotalGasto(BigDecimal materiaisConsumidos, BigDecimal outrosCustos) {
        return materiaisConsumidos.add(outrosCustos).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularResultado(BigDecimal faturamentoTotal, BigDecimal totalGasto) {
        return faturamentoTotal.subtract(totalGasto).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularMargem(BigDecimal resultado, BigDecimal faturamentoTotal) {
        return faturamentoTotal.signum() == 0 ? zero()
                : resultado.multiply(BigDecimal.valueOf(100)).divide(faturamentoTotal, 2, RoundingMode.HALF_UP);
    }

    private void validarMateriais(BigDecimal saldoInicial, BigDecimal compras, BigDecimal saldoFinal) {
        validarNaoNegativo(saldoInicial, "Saldo inicial de materiais");
        validarNaoNegativo(compras, "Compras da produção");
        validarNaoNegativo(saldoFinal, "Saldo final de materiais");
        if (saldoFinal.compareTo(calcularRecursosDisponiveis(saldoInicial, compras)) > 0)
            throw new BusinessException("O saldo final não pode ser maior que o saldo inicial somado às compras.");
    }

    private void validarNaoNegativo(BigDecimal valor, String campo) {
        if (valor.signum() < 0) throw new BusinessException(campo + " não pode ser negativo.");
    }

    private Producao buscarEntidade(Long id) { return producaoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Produção não encontrada.")); }
    private void validarPeriodo(LocalDate inicio, LocalDate fim) { if (inicio != null && fim != null && inicio.isAfter(fim)) throw new BusinessException("A data inicial não pode ser posterior à data final."); }
    private void duplicada() { throw new BusinessException("Já existe uma produção cadastrada para esta data."); }
    private BigDecimal moeda(BigDecimal v) { return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal zero() { return BigDecimal.ZERO.setScale(2); }
    private record ValoresMateriais(BigDecimal saldoInicial, BigDecimal compras, BigDecimal saldoFinal) {}
}
