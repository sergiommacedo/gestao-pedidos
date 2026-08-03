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
        p.setDataProducao(r.dataProducao()); p.setValorIngredientes(moeda(r.valorIngredientes()));
        p.setValorEmbalagens(moeda(r.valorEmbalagens())); p.setValorGasEnergia(moeda(r.valorGasEnergia()));
        p.setValorOutros(moeda(r.valorOutros())); p.setObservacao(r.observacao() == null ? null : r.observacao().trim());
    }

    private ProducaoResumoResponse montarResumo(Producao p, PedidoRepository.ResumoFinanceiroProducao f) {
        ProducaoResponse resposta = mapear(p); BigDecimal produtos = f == null ? zero() : moeda(f.getFaturamentoProdutos());
        BigDecimal taxas = f == null ? zero() : moeda(f.getTaxasEntrega());
        BigDecimal faturamento = produtos.add(taxas).setScale(2, RoundingMode.HALF_UP);
        BigDecimal resultado = faturamento.subtract(resposta.totalGasto()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal margem = faturamento.signum() == 0 ? zero()
                : resultado.multiply(BigDecimal.valueOf(100)).divide(faturamento, 2, RoundingMode.HALF_UP);
        return ProducaoResumoResponse.builder().producao(resposta).pedidosValidos(f == null || f.getPedidosValidos() == null ? 0 : f.getPedidosValidos())
                .faturamentoProdutos(produtos).taxasEntrega(taxas).faturamentoTotal(faturamento)
                .totalGasto(resposta.totalGasto()).resultadoBrutoEstimado(resultado).margemBrutaEstimada(margem).build();
    }

    private ProducaoResponse mapear(Producao p) {
        BigDecimal ingredientes=moeda(p.getValorIngredientes()), embalagens=moeda(p.getValorEmbalagens());
        BigDecimal gas=moeda(p.getValorGasEnergia()), outros=moeda(p.getValorOutros());
        return ProducaoResponse.builder().id(p.getId()).dataProducao(p.getDataProducao())
                .valorIngredientes(ingredientes).valorEmbalagens(embalagens).valorGasEnergia(gas).valorOutros(outros)
                .totalGasto(ingredientes.add(embalagens).add(gas).add(outros).setScale(2, RoundingMode.HALF_UP))
                .observacao(p.getObservacao()).criadoEm(p.getCriadoEm()).atualizadoEm(p.getAtualizadoEm()).build();
    }

    private Producao buscarEntidade(Long id) { return producaoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Produção não encontrada.")); }
    private void validarPeriodo(LocalDate inicio, LocalDate fim) { if (inicio != null && fim != null && inicio.isAfter(fim)) throw new BusinessException("A data inicial não pode ser posterior à data final."); }
    private void duplicada() { throw new BusinessException("Já existe uma produção cadastrada para esta data."); }
    private BigDecimal moeda(BigDecimal v) { return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal zero() { return BigDecimal.ZERO.setScale(2); }
}
