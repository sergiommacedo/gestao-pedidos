package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.producao.*;
import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.*;
import br.com.sergio.gestaopedidos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional
public class ProducaoService {
    private final ProducaoRepository producoes;
    private final ProdutoRepository produtos;
    private final FichaTecnicaRepository fichas;
    private final MovimentacaoEstoqueRepository movimentos;
    private final SaldoEstoqueRepository saldos;
    private final EstoqueService estoque;

    @Transactional(readOnly=true) public Page<ProducaoResumoResponse> listar(LocalDate inicio,LocalDate fim,Pageable pageable){validarPeriodo(inicio,fim);return producoes.buscarPorPeriodo(inicio,fim,pageable).map(p->new ProducaoResumoResponse(mapear(p),moeda(p.getCustoTotal())));}
    @Transactional(readOnly=true) public ProducaoResponse buscarPorId(Long id){return mapear(buscar(id));}
    @Transactional(readOnly=true) public ProducaoDetalhesResponse buscarDetalhes(Long id){Producao p=buscar(id);Map<Insumo,BigDecimal> consumos=p.statusEfetivo()==StatusProducao.CONFIRMADA?consumosMovimentados(p):consumosPrevistos(p);return detalhes(p,consumos);}
    @Transactional(readOnly=true) public List<Produto> produtosDisponiveis(){return produtos.buscarProduzidosAtivosComFichaAtiva();}

    public ProducaoResponse salvar(ProducaoRequest r){if(producoes.existsByDataProducao(r.dataProducao()))duplicada();Producao p=new Producao();aplicar(p,r);return mapear(salvarSeguro(p));}
    public ProducaoResponse atualizar(Long id,ProducaoRequest r){Producao p=buscar(id);validarRascunho(p);if(producoes.existsByDataProducaoAndIdNot(r.dataProducao(),id))duplicada();aplicar(p,r);return mapear(salvarSeguro(p));}
    public void excluir(Long id){Producao p=buscar(id);validarRascunho(p);producoes.delete(p);}
    public ProducaoDetalhesResponse confirmar(Long id){
        Producao p=producoes.bloquearDetalhada(id).orElseThrow(()->new ResourceNotFoundException("Produção não encontrada."));validarRascunho(p);
        Map<Long,FichaTecnica> fs=validarPreparacoesEFichas(p);Map<Long,Map<Insumo,BigDecimal>> porProduto=calcularConsumos(p,fs);
        BigDecimal adicionais=moeda(p.getValorGasEnergia()).add(moeda(p.getValorOutros())).setScale(2);
        BigDecimal consumido=estoque.processarProducao(p,porProduto,adicionais);
        p.setValorInsumosConsumidos(consumido);p.setCustoTotal(consumido.add(adicionais).setScale(2,RoundingMode.HALF_UP));p.setStatus(StatusProducao.CONFIRMADA);p.setConfirmadaEm(LocalDateTime.now());
        producoes.saveAndFlush(p);return detalhes(p,agrupar(porProduto));
    }

    private void aplicar(Producao p,ProducaoRequest r){if(r.dataProducao()==null)throw new BusinessException("Data da produção é obrigatória.");p.setDataProducao(r.dataProducao());p.setValorGasEnergia(naoNegativo(r.valorGasEnergia(),"Gás/Energia"));p.setValorOutros(naoNegativo(r.valorOutros(),"Outros"));p.setObservacao(texto(r.observacao()));aplicarItens(p,r.itens());}
    private void aplicarItens(Producao p,List<ItemProducaoRequest> requests){if(requests==null||requests.isEmpty())throw new BusinessException("Adicione ao menos uma preparação produzida.");Set<Long> ids=new HashSet<>();List<ItemProducao> itens=new ArrayList<>();for(ItemProducaoRequest r:requests){if(r==null||r.getProdutoId()==null)throw new BusinessException("Selecione a preparação produzida.");if(!ids.add(r.getProdutoId()))throw new BusinessException("A mesma preparação não pode ser repetida.");Produto produto=produtos.findById(r.getProdutoId()).orElseThrow(()->new ResourceNotFoundException("Produto não encontrado."));validarPreparacao(produto);BigDecimal q=quantidade(r.getQuantidade(),produto);FichaTecnica f=fichas.findByProdutoId(produto.getId()).orElseThrow(()->new BusinessException("A preparação "+produto.getNome()+" não possui Ficha Técnica."));if(!Boolean.TRUE.equals(f.getAtiva())||f.getItens().isEmpty())throw new BusinessException("A Ficha Técnica de "+produto.getNome()+" deve estar ativa e possuir itens.");itens.add(ItemProducao.builder().produto(produto).nomeHistorico(produto.getNome()).unidadeHistorica(unidade(produto)).quantidade(q).build());}p.limparItens();itens.forEach(p::adicionarItem);}
    private Map<Long,FichaTecnica> validarPreparacoesEFichas(Producao p){Set<Long> ids=p.getItens().stream().map(i->i.getProduto().getId()).collect(Collectors.toSet());p.getItens().forEach(i->{validarPreparacao(i.getProduto());quantidade(i.getQuantidade(),i.getProduto());});Map<Long,FichaTecnica> fs=fichas.buscarAtivasPorProdutos(ids).stream().collect(Collectors.toMap(f->f.getProduto().getId(),Function.identity()));for(ItemProducao i:p.getItens())if(!fs.containsKey(i.getProduto().getId())||fs.get(i.getProduto().getId()).getItens().isEmpty())throw new BusinessException("A preparação "+i.getProduto().getNome()+" não possui Ficha Técnica ativa com itens.");return fs;}
    private Map<Long,Map<Insumo,BigDecimal>> calcularConsumos(Producao p,Map<Long,FichaTecnica> fs){Map<Long,Map<Insumo,BigDecimal>> resultado=new LinkedHashMap<>();for(ItemProducao ip:p.getItens()){Map<Insumo,BigDecimal> cs=new LinkedHashMap<>();for(ItemFichaTecnica item:fs.get(ip.getProduto().getId()).getItens())cs.merge(item.getInsumo(),ip.getQuantidade().multiply(item.getQuantidade()).setScale(3,RoundingMode.HALF_UP),BigDecimal::add);resultado.put(ip.getProduto().getId(),cs);}return resultado;}
    private Map<Insumo,BigDecimal> consumosPrevistos(Producao p){try{return agrupar(calcularConsumos(p,validarPreparacoesEFichas(p)));}catch(BusinessException e){return Map.of();}}
    private Map<Insumo,BigDecimal> consumosMovimentados(Producao p){Map<Insumo,BigDecimal> r=new LinkedHashMap<>();movimentos.findByProducaoIdOrderByIdAsc(p.getId()).stream().filter(m->m.getTipo()==TipoMovimentacaoEstoque.SAIDA_CONSUMO_PRODUCAO&&m.getInsumo()!=null).forEach(m->r.merge(m.getInsumo(),m.getQuantidade(),BigDecimal::add));return r;}
    private Map<Insumo,BigDecimal> agrupar(Map<Long,Map<Insumo,BigDecimal>> origem){Map<Insumo,BigDecimal> r=new LinkedHashMap<>();origem.values().forEach(m->m.forEach((i,q)->r.merge(i,q,BigDecimal::add)));return r;}
    private ProducaoDetalhesResponse detalhes(Producao p,Map<Insumo,BigDecimal> consumos){Map<Long,MovimentacaoEstoque> saidas=movimentos.findByProducaoIdOrderByIdAsc(p.getId()).stream().filter(m->m.getTipo()==TipoMovimentacaoEstoque.SAIDA_CONSUMO_PRODUCAO&&m.getInsumo()!=null).collect(Collectors.toMap(m->m.getInsumo().getId(),Function.identity(),(a,b)->a));List<ConsumoInsumoProducaoResponse> cs=consumos.entrySet().stream().map(e->{MovimentacaoEstoque m=saidas.get(e.getKey().getId());SaldoEstoque s=saldos.buscarSaldo(TipoItemEstoque.INSUMO,e.getKey().getId()).orElse(null);BigDecimal custo=m!=null?m.getCustoUnitario():s==null?zero6():s.getCustoMedioAtual();return ConsumoInsumoProducaoResponse.builder().insumoId(e.getKey().getId()).insumoNome(e.getKey().getNome()).unidade(e.getKey().getUnidadeMedida()).quantidade(e.getValue()).custoMedio(custo).custoTotal(e.getValue().multiply(custo).setScale(2,RoundingMode.HALF_UP)).build();}).toList();List<MovimentacaoProducaoResponse> ms=movimentos.findByProducaoIdOrderByIdAsc(p.getId()).stream().map(m->MovimentacaoProducaoResponse.builder().id(m.getId()).data(m.getDataMovimentacao()).item(m.getNomeHistorico()).categoria(m.getTipoItem()).tipo(m.getTipo()).unidade(m.getUnidadeHistorica()).quantidade(m.getQuantidade()).custoUnitario(m.getCustoUnitario()).valorTotal(m.getValorTotal()).saldoAnterior(m.getSaldoAnterior()).saldoPosterior(m.getSaldoPosterior()).build()).toList();BigDecimal total=cs.stream().map(ConsumoInsumoProducaoResponse::custoTotal).reduce(zero2(),BigDecimal::add);return ProducaoDetalhesResponse.builder().resumo(new ProducaoResumoResponse(mapear(p),moeda(p.getCustoTotal()))).produtos(mapearItens(p)).consumos(cs).movimentacoes(ms).produtosDistintos(p.getItens().size()).insumosDistintos(cs.size()).quantidadeTotal(p.getItens().stream().map(ItemProducao::getQuantidade).reduce(BigDecimal.ZERO,BigDecimal::add)).totalConsumido(total).build();}
    private ProducaoResponse mapear(Producao p){BigDecimal gas=moeda(p.getValorGasEnergia()),outros=moeda(p.getValorOutros());return ProducaoResponse.builder().id(p.getId()).dataProducao(p.getDataProducao()).valorInsumosConsumidos(moeda(p.getValorInsumosConsumidos())).valorGasEnergia(gas).valorOutros(outros).gastosAdicionais(gas.add(outros).setScale(2)).custoTotal(moeda(p.getCustoTotal())).observacao(p.getObservacao()).criadoEm(p.getCriadoEm()).atualizadoEm(p.getAtualizadoEm()).status(p.statusEfetivo()).confirmadaEm(p.getConfirmadaEm()).itens(mapearItens(p)).quantidadeTotal(p.getItens().stream().map(ItemProducao::getQuantidade).reduce(BigDecimal.ZERO,BigDecimal::add)).build();}
    private List<ItemProducaoResponse> mapearItens(Producao p){return p.getItens().stream().map(i->ItemProducaoResponse.builder().id(i.getId()).produtoId(i.getProduto().getId()).produtoNome(i.getNomeHistorico()).unidade(i.getUnidadeHistorica()).quantidade(i.getQuantidade()).custoTotal(moeda(i.getCustoTotal())).custoUnitario(i.getCustoUnitario()==null?zero6():i.getCustoUnitario()).fichaAtiva(true).build()).toList();}
    private void validarPreparacao(Produto p){if(p.getTipoProduto()!=TipoProduto.PREPARACAO_PRODUZIDA)throw new BusinessException("Somente preparações produzidas podem ser incluídas na Produção.");if(!Boolean.TRUE.equals(p.getAtivo()))throw new BusinessException("A preparação "+p.getNome()+" está inativa.");}
    private BigDecimal quantidade(BigDecimal q,Produto p){if(q==null||q.signum()<=0||q.stripTrailingZeros().scale()>3)throw new BusinessException("Quantidade produzida deve ser maior que zero e ter no máximo três casas decimais.");if(p.getUnidadeVenda()==UnidadeVenda.UNIDADE&&q.stripTrailingZeros().scale()>0)throw new BusinessException("Quantidade em unidade deve ser inteira.");return q.setScale(3,RoundingMode.UNNECESSARY);}
    private UnidadeMedida unidade(Produto p){return p.getUnidadeVenda()==UnidadeVenda.UNIDADE?UnidadeMedida.UNIDADE:UnidadeMedida.QUILOGRAMA;}
    private Producao salvarSeguro(Producao p){try{return producoes.saveAndFlush(p);}catch(DataIntegrityViolationException e){throw new BusinessException("Já existe uma produção cadastrada para esta data.");}}
    private Producao buscar(Long id){return producoes.buscarDetalhada(id).orElseThrow(()->new ResourceNotFoundException("Produção não encontrada."));}
    private void validarRascunho(Producao p){if(p.statusEfetivo()!=StatusProducao.RASCUNHO)throw new BusinessException("Produção confirmada não pode ser alterada.");}
    private void validarPeriodo(LocalDate i,LocalDate f){if(i!=null&&f!=null&&i.isAfter(f))throw new BusinessException("A data inicial não pode ser posterior à data final.");}
    private void duplicada(){throw new BusinessException("Já existe uma produção cadastrada para esta data.");}
    private BigDecimal naoNegativo(BigDecimal v,String nome){v=moeda(v);if(v.signum()<0)throw new BusinessException(nome+" não pode ser negativo.");return v;}
    private BigDecimal moeda(BigDecimal v){return(v==null?BigDecimal.ZERO:v).setScale(2,RoundingMode.HALF_UP);}private BigDecimal zero2(){return BigDecimal.ZERO.setScale(2);}private BigDecimal zero6(){return BigDecimal.ZERO.setScale(6);}private String texto(String s){return s==null||s.trim().isEmpty()?null:s.trim();}
}
