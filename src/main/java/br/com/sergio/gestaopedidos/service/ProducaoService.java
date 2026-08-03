package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.producao.*;
import br.com.sergio.gestaopedidos.entity.Producao;
import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.*;
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
    private final ProdutoRepository produtoRepository;
    private final FichaTecnicaRepository fichaTecnicaRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final EstoqueService estoqueService;

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
        validarRascunho(producao);
        if (producaoRepository.existsByDataProducaoAndIdNot(request.dataProducao(), id)) duplicada();
        aplicar(producao, request);
        return mapear(salvarSeguro(producao));
    }

    public void excluir(Long id) { Producao p=buscarEntidade(id);validarRascunho(p);producaoRepository.delete(p); }

    public ProducaoDetalhesResponse confirmar(Long id){Producao p=producaoRepository.bloquearDetalhada(id).orElseThrow(()->new ResourceNotFoundException("Produção não encontrada."));validarRascunho(p);Map<Long,FichaTecnica> fichas=validarProdutosEFichas(p);Map<Insumo,BigDecimal> consumos=agruparConsumos(p,fichas);estoqueService.processarProducao(p,consumos);p.setStatus(StatusProducao.CONFIRMADA);p.setConfirmadaEm(java.time.LocalDateTime.now());producaoRepository.saveAndFlush(p);return montarDetalhes(p,consumos);}

    @Transactional(readOnly=true) public ProducaoDetalhesResponse buscarDetalhes(Long id){Producao p=producaoRepository.buscarDetalhada(id).orElseThrow(()->new ResourceNotFoundException("Produção não encontrada."));Map<Insumo,BigDecimal> consumos=p.getItens().isEmpty()?Map.of():agruparConsumosExistentes(p);return montarDetalhes(p,consumos);}

    @Transactional(readOnly=true) public List<Produto> produtosDisponiveis(){return produtoRepository.buscarProduzidosAtivosComFichaAtiva();}

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
        aplicarItens(p,r.itens());
    }

    private void aplicarItens(Producao p,List<ItemProducaoRequest> itens){if(itens==null||itens.isEmpty())throw new BusinessException("Adicione ao menos um produto produzido.");Set<Long> ids=new HashSet<>();List<ItemProducao> novos=new ArrayList<>();for(ItemProducaoRequest r:itens){if(r==null||r.getProdutoId()==null)throw new BusinessException("Selecione o produto produzido.");if(!ids.add(r.getProdutoId()))throw new BusinessException("O mesmo produto não pode ser informado duas vezes.");Produto produto=produtoRepository.findById(r.getProdutoId()).orElseThrow(()->new ResourceNotFoundException("Produto não encontrado."));validarProduto(produto);BigDecimal q=quantidade(r.getQuantidade(),produto);FichaTecnica f=fichaTecnicaRepository.findByProdutoId(produto.getId()).orElseThrow(()->new BusinessException("O produto "+produto.getNome()+" não possui Ficha Técnica."));if(!Boolean.TRUE.equals(f.getAtiva()))throw new BusinessException("A Ficha Técnica de "+produto.getNome()+" está inativa.");novos.add(ItemProducao.builder().produto(produto).nomeHistorico(produto.getNome()).unidadeHistorica(unidade(produto)).quantidade(q).build());}p.limparItens();novos.forEach(p::adicionarItem);}

    private Map<Long,FichaTecnica> validarProdutosEFichas(Producao p){if(p.getItens()==null||p.getItens().isEmpty())throw new BusinessException("Adicione ao menos um produto produzido.");Set<Long> ids=new HashSet<>();for(ItemProducao i:p.getItens()){validarProduto(i.getProduto());quantidade(i.getQuantidade(),i.getProduto());if(!ids.add(i.getProduto().getId()))throw new BusinessException("O mesmo produto não pode ser informado duas vezes.");}Map<Long,FichaTecnica> fs=fichaTecnicaRepository.buscarAtivasPorProdutos(ids).stream().collect(Collectors.toMap(f->f.getProduto().getId(),Function.identity()));for(ItemProducao i:p.getItens())if(!fs.containsKey(i.getProduto().getId()))throw new BusinessException("O produto "+i.getProduto().getNome()+" não possui Ficha Técnica ativa.");return fs;}
    private Map<Insumo,BigDecimal> agruparConsumos(Producao p,Map<Long,FichaTecnica> fs){Map<Insumo,BigDecimal> m=new LinkedHashMap<>();for(ItemProducao ip:p.getItens())for(ItemFichaTecnica it:fs.get(ip.getProduto().getId()).getItens())m.merge(it.getInsumo(),ip.getQuantidade().multiply(it.getQuantidade()),BigDecimal::add);m.replaceAll((i,q)->q.setScale(3,RoundingMode.HALF_UP));return m;}
    private Map<Insumo,BigDecimal> agruparConsumosExistentes(Producao p){Map<Long,FichaTecnica> fs=fichaTecnicaRepository.buscarAtivasPorProdutos(p.getItens().stream().map(i->i.getProduto().getId()).toList()).stream().collect(Collectors.toMap(f->f.getProduto().getId(),Function.identity()));return fs.size()==p.getItens().size()?agruparConsumos(p,fs):Map.of();}
    private void validarProduto(Produto p){if(p.getTipoProduto()!=TipoProduto.PRODUZIDO)throw new BusinessException("Somente produtos produzidos podem ser incluídos na Produção.");if(!Boolean.TRUE.equals(p.getAtivo()))throw new BusinessException("O produto "+p.getNome()+" está inativo.");}
    private BigDecimal quantidade(BigDecimal q,Produto p){if(q==null||q.signum()<=0)throw new BusinessException("Quantidade produzida deve ser maior que zero.");if(q.stripTrailingZeros().scale()>3)throw new BusinessException("Quantidade deve ter no máximo três casas decimais.");if(p.getUnidadeVenda()==UnidadeVenda.UNIDADE&&q.stripTrailingZeros().scale()>0)throw new BusinessException("Quantidade em unidade deve ser inteira.");return q.setScale(3,RoundingMode.UNNECESSARY);}
    private UnidadeMedida unidade(Produto p){return p.getUnidadeVenda()==UnidadeVenda.UNIDADE?UnidadeMedida.UNIDADE:UnidadeMedida.QUILOGRAMA;}
    private void validarRascunho(Producao p){if(p.statusEfetivo()==StatusProducao.CONFIRMADA)throw new BusinessException("Produção confirmada não pode ser alterada.");}

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
                .observacao(p.getObservacao()).criadoEm(p.getCriadoEm()).atualizadoEm(p.getAtualizadoEm())
                .status(p.statusEfetivo()).confirmadaEm(p.getConfirmadaEm()).itens(mapearItens(p))
                .quantidadeTotal(p.getItens().stream().map(ItemProducao::getQuantidade).reduce(BigDecimal.ZERO,BigDecimal::add)).build();
    }

    private List<ItemProducaoResponse> mapearItens(Producao p){return p.getItens().stream().map(i->ItemProducaoResponse.builder().id(i.getId()).produtoId(i.getProduto().getId()).produtoNome(i.getNomeHistorico()).unidade(i.getUnidadeHistorica()).quantidade(i.getQuantidade()).fichaAtiva(true).build()).toList();}
    private ProducaoDetalhesResponse montarDetalhes(Producao p,Map<Insumo,BigDecimal> consumos){ProducaoResumoResponse resumo=montarResumo(p,pedidoRepository.resumirFinanceiroProducao(p.getDataProducao(),StatusPedido.CANCELADO).orElse(null));List<ConsumoInsumoProducaoResponse> cs=consumos.entrySet().stream().map(e->ConsumoInsumoProducaoResponse.builder().insumoId(e.getKey().getId()).insumoNome(e.getKey().getNome()).unidade(e.getKey().getUnidadeMedida()).quantidade(e.getValue()).build()).toList();List<MovimentacaoProducaoResponse> ms=movimentacaoRepository.findByProducaoIdOrderByIdAsc(p.getId()).stream().map(m->MovimentacaoProducaoResponse.builder().id(m.getId()).data(m.getDataMovimentacao()).item(m.getNomeHistorico()).categoria(m.getTipoItem()).tipo(m.getTipo()).unidade(m.getUnidadeHistorica()).quantidade(m.getQuantidade()).saldoAnterior(m.getSaldoAnterior()).saldoPosterior(m.getSaldoPosterior()).build()).toList();return ProducaoDetalhesResponse.builder().resumo(resumo).produtos(mapearItens(p)).consumos(cs).movimentacoes(ms).produtosDistintos(p.getItens().size()).insumosDistintos(cs.size()).quantidadeTotal(p.getItens().stream().map(ItemProducao::getQuantidade).reduce(BigDecimal.ZERO,BigDecimal::add)).build();}

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

    private Producao buscarEntidade(Long id) { return producaoRepository.buscarDetalhada(id).orElseThrow(() -> new ResourceNotFoundException("Produção não encontrada.")); }
    private void validarPeriodo(LocalDate inicio, LocalDate fim) { if (inicio != null && fim != null && inicio.isAfter(fim)) throw new BusinessException("A data inicial não pode ser posterior à data final."); }
    private void duplicada() { throw new BusinessException("Já existe uma produção cadastrada para esta data."); }
    private BigDecimal moeda(BigDecimal v) { return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal zero() { return BigDecimal.ZERO.setScale(2); }
    private record ValoresMateriais(BigDecimal saldoInicial, BigDecimal compras, BigDecimal saldoFinal) {}
}
