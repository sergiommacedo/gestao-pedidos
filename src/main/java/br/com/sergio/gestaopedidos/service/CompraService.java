package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.compra.*;
import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.*;
import br.com.sergio.gestaopedidos.repository.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional
public class CompraService {
    private final CompraRepository compraRepository;
    private final InsumoService insumoService;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;

    @Transactional(readOnly=true)
    public Page<CompraResponse> listar(LocalDate inicio,LocalDate fim,String fornecedor,TipoCompra tipo,Pageable pageable){
        validarPeriodo(inicio,fim);
        return compraRepository.buscar(inicio,fim,normalizar(fornecedor),tipo,pageable).map(r->CompraResponse.builder()
                .id(r.getId()).tipoCompra(r.getTipoCompra()).dataCompra(r.getDataCompra()).fornecedor(r.getFornecedor())
                .valorTotal(moeda(r.getValorTotal())).quantidadeItens(r.getQuantidadeItens().intValue())
                .atualizadoEm(r.getAtualizadoEm()).status(r.getStatus()).itens(List.of()).build());
    }
    @Transactional(readOnly=true)
    public ResumoComprasResponse resumir(LocalDate inicio,LocalDate fim,LocalDate hoje){
        validarPeriodo(inicio,fim);LocalDate referencia=hoje==null?LocalDate.now():hoje;
        CompraRepository.ResumoOperacional r=compraRepository.resumirOperacao(referencia,referencia.withDayOfMonth(1),
                referencia.withDayOfMonth(referencia.lengthOfMonth()),inicio,fim);
        return new ResumoComprasResponse(numero(r==null?null:r.getComprasHoje()),moeda(r==null?null:r.getTotalHoje()),
                moeda(r==null?null:r.getTotalMes()),moeda(r==null?null:r.getTotalPeriodo()),inicio!=null||fim!=null);
    }
    @Transactional(readOnly=true)
    public AnalisePrecosCompraResponse analisarPrecos(TipoItemEstoque tipoItem,Long referenciaId,UnidadeMedida unidade,
            String fornecedor,TipoItemEstoque categoria,LocalDate inicio,LocalDate fim){
        validarPeriodo(inicio,fim);validarCategoria(categoria);validarSelecaoItem(tipoItem,referenciaId,unidade);
        String fornecedorNormalizado=normalizar(fornecedor);
        List<AnalisePrecosCompraResponse.FornecedorItem> linhas=compraRepository.analisarPrecos(tipoItem==null?"":tipoItem.name(),
                referenciaId,unidade==null?"":unidade.name(),fornecedorNormalizado,categoria==null?"":categoria.name(),inicio,fim)
                .stream().map(this::mapearAnalise).toList();
        Map<ChaveAnalise,List<AnalisePrecosCompraResponse.FornecedorItem>> grupos=linhas.stream()
                .collect(Collectors.groupingBy(l->new ChaveAnalise(l.categoria(),l.referenciaId(),l.item(),l.unidade()),LinkedHashMap::new,Collectors.toList()));
        List<AnalisePrecosCompraResponse.ComparativoItem> comparativos=grupos.entrySet().stream()
                .map(e->comparar(e.getKey(),e.getValue())).toList();
        return new AnalisePrecosCompraResponse(linhas,comparativos);
    }
    @Transactional(readOnly=true) public List<OpcaoAnalisePrecoResponse> buscarItensAnalise(String termo){return compraRepository.buscarOpcoesItensAnalise(normalizar(termo),PageRequest.of(0,20)).stream().map(this::mapearOpcao).toList();}
    @Transactional(readOnly=true) public List<String> buscarFornecedoresAnalise(String termo){return compraRepository.buscarFornecedoresAnalise(normalizar(termo),PageRequest.of(0,20));}
    @Transactional(readOnly=true) public HistoricoPrecoPageResponse buscarHistoricoPrecos(TipoItemEstoque tipoItem,Long referenciaId,
            UnidadeMedida unidade,String fornecedor,LocalDate inicio,LocalDate fim,int pagina){
        validarPeriodo(inicio,fim);validarSelecaoItem(tipoItem,referenciaId,unidade);
        var identidade=compraRepository.buscarIdentidadeItemAnalise(tipoItem.name(),referenciaId,unidade.name())
                .orElseThrow(()->new ResourceNotFoundException("Histórico de compra não encontrado."));
        Page<CompraRepository.HistoricoPreco> resultado=compraRepository.buscarHistoricoPrecos(tipoItem.name(),referenciaId,
                unidade.name(),normalizar(fornecedor),inicio,fim,PageRequest.of(Math.max(0,pagina),10));
        return new HistoricoPrecoPageResponse(identidade.getItemNome(),tipoItem,unidade,resultado.getContent().stream()
                .map(h->new AnalisePrecosCompraResponse.HistoricoItem(h.getDataCompra(),h.getFornecedor(),h.getQuantidade(),h.getValorPago(),h.getPrecoUnitario())).toList(),
                resultado.getNumber(),resultado.getTotalPages(),resultado.getTotalElements(),resultado.isFirst(),resultado.isLast());
    }
    @Transactional(readOnly=true) public CompraResponse buscarPorId(Long id){return mapear(buscarEntidade(id));}

    public CompraResponse salvar(CompraRequest request){
        validarCabecalho(request); validarIdsEDuplicidade(request.getItens(),Set.of());
        Compra compra=new Compra();aplicarCabecalho(compra,request);
        for(ItemCompraRequest recebido:request.getItens())compra.adicionarItem(criarItem(recebido));
        compra.recalcularTipoCompra();compra.recalcularTotal();compra=compraRepository.saveAndFlush(compra);estoqueService.processarCompra(compra);return mapear(compra);
    }

    public CompraResponse atualizar(Long id,CompraRequest request){
        validarCabecalho(request);Compra compra=buscarEntidade(id);
        if(estoqueService.compraMovimentada(id)){validarFinanceiroImutavel(compra,request);compra.setFornecedor(normalizarOpcional(request.getFornecedor()));compra.setObservacao(normalizarOpcional(request.getObservacao()));return mapear(compraRepository.save(compra));}
        aplicarCabecalho(compra,request);
        Map<Long,ItemCompra> existentes=compra.getItens().stream().collect(Collectors.toMap(ItemCompra::getId,Function.identity()));
        validarIdsEDuplicidade(request.getItens(),existentes.keySet());Set<Long> mantidos=new HashSet<>();
        for(ItemCompraRequest recebido:request.getItens()){
            if(recebido.getId()==null){compra.adicionarItem(criarItem(recebido));continue;}
            ItemCompra item=existentes.get(recebido.getId());mantidos.add(item.getId());
            if(item.getTipoItem()!=recebido.getTipoItem()||!Objects.equals(referenciaId(item),recebido.getReferenciaId())){ItemCompra novo=criarItem(recebido);item.setInsumo(novo.getInsumo());item.setProduto(novo.getProduto());item.setNomeHistorico(novo.getNomeHistorico());item.setUnidadeHistorica(novo.getUnidadeHistorica());}
            aplicarValores(item,recebido);
        }
        for(Iterator<ItemCompra> it=compra.getItens().iterator();it.hasNext();){ItemCompra item=it.next();if(item.getId()!=null&&!mantidos.contains(item.getId())){it.remove();item.setCompra(null);}}
        compra.recalcularTipoCompra();compra.recalcularTotal();return mapear(compraRepository.save(compra));
    }

    public void excluir(Long id){if(estoqueService.compraMovimentada(id))throw new BusinessException("Compra com movimentação de estoque não pode ser excluída. Utilize o estorno.");compraRepository.delete(buscarEntidade(id));}
    public void estornar(Long id){Compra compra=buscarEntidade(id);estoqueService.estornarCompra(compra);compraRepository.save(compra);}
    @Transactional(readOnly=true) public boolean compraMovimentada(Long id){return estoqueService.compraMovimentada(id);}

    private ItemCompra criarItem(ItemCompraRequest request){
        ItemCompra item=new ItemCompra();
        if(request.getTipoItem()==TipoItemEstoque.INSUMO){Insumo i=insumoService.buscarEntidade(request.getReferenciaId());if(!Boolean.TRUE.equals(i.getAtivo()))throw new BusinessException("Este insumo não está disponível para novas compras.");item.setInsumo(i);item.setProduto(null);item.setNomeHistorico(i.getNome());item.setUnidadeHistorica(i.getUnidadeMedida());}
        else if(request.getTipoItem()==TipoItemEstoque.PRODUTO_REVENDA){Produto p=produtoService.buscarEntidadePorId(request.getReferenciaId());if(p.getTipoProduto()!=TipoProduto.PRODUTO_REVENDA)throw new BusinessException("Somente produtos de revenda podem ser incluídos nesta compra.");if(!Boolean.TRUE.equals(p.getAtivo()))throw new BusinessException("Este produto não está disponível para novas compras.");item.setProduto(p);item.setInsumo(null);item.setNomeHistorico(p.getNome());item.setUnidadeHistorica(p.getUnidadeVenda()==UnidadeVenda.UNIDADE?UnidadeMedida.UNIDADE:UnidadeMedida.QUILOGRAMA);}
        else throw new BusinessException("Somente Insumo ou Produto de revenda pode ser comprado.");
        aplicarValores(item,request);validarReferenciaExclusiva(item);return item;
    }
    private void aplicarValores(ItemCompra item,ItemCompraRequest request){BigDecimal q=quantidade(request.getQuantidade(),item.getUnidadeHistorica()),v=valor(request.getValorTotalItem());item.setQuantidade(q);item.setValorTotalItem(v);item.setCustoUnitario(v.divide(q,6,RoundingMode.HALF_UP));}
    private void validarReferenciaExclusiva(ItemCompra item){if((item.getInsumo()==null)==(item.getProduto()==null))throw new BusinessException("O item deve referenciar somente um Insumo ou Produto de revenda.");}
    private void validarFinanceiroImutavel(Compra compra,CompraRequest request){if(!Objects.equals(compra.getDataCompra(),request.getDataCompra())||request.getItens()==null||compra.getItens().size()!=request.getItens().size())imutavel();Map<Long,ItemCompraRequest> enviados=request.getItens().stream().filter(i->i.getId()!=null).collect(Collectors.toMap(ItemCompraRequest::getId,Function.identity(),(a,b)->{imutavel();return a;}));for(ItemCompra atual:compra.getItens()){ItemCompraRequest recebido=enviados.get(atual.getId());if(recebido==null||recebido.getTipoItem()!=atual.getTipoItem()||!Objects.equals(recebido.getReferenciaId(),referenciaId(atual))||atual.getQuantidade().compareTo(recebido.getQuantidade())!=0||atual.getValorTotalItem().compareTo(recebido.getValorTotalItem())!=0)imutavel();}}
    private void imutavel(){throw new BusinessException("Esta compra já movimentou o estoque. Somente fornecedor e observação podem ser alterados.");}
    private void validarCabecalho(CompraRequest r){if(r.getDataCompra()==null)throw new BusinessException("Data da compra é obrigatória.");if(r.getItens()==null||r.getItens().isEmpty())throw new BusinessException("Adicione ao menos um item à compra.");}
    private void validarIdsEDuplicidade(List<ItemCompraRequest> itens,Set<Long> idsExistentes){Set<Long> ids=new HashSet<>();Set<String>referencias=new HashSet<>();for(ItemCompraRequest i:itens){if(i.getId()!=null&&(!ids.add(i.getId())||!idsExistentes.contains(i.getId())))throw new BusinessException("Item informado não pertence a esta compra.");if(i.getTipoItem()==null||i.getReferenciaId()==null)throw new BusinessException("Selecione o item comprado.");if(!referencias.add(i.getTipoItem()+":"+i.getReferenciaId()))throw new BusinessException("Este item já foi adicionado à compra.");}}
    private void aplicarCabecalho(Compra c,CompraRequest r){c.setDataCompra(r.getDataCompra());c.setFornecedor(normalizarOpcional(r.getFornecedor()));c.setObservacao(normalizarOpcional(r.getObservacao()));}
    private Long referenciaId(ItemCompra i){return i.getInsumo()!=null?i.getInsumo().getId():i.getProduto().getId();}
    private Compra buscarEntidade(Long id){return compraRepository.buscarDetalhada(id).orElseThrow(()->new ResourceNotFoundException("Compra não encontrada."));}
    private CompraResponse mapear(Compra c){List<ItemCompraResponse> itens=c.getItens().stream().map(i->ItemCompraResponse.builder().id(i.getId()).referenciaId(referenciaId(i)).tipoItem(i.getTipoItem()).nomeHistorico(i.getNomeHistorico()).categoria(i.getTipoItem().getDescricao()).unidadeHistorica(i.getUnidadeHistorica()).quantidade(i.getQuantidade()).valorTotalItem(i.getValorTotalItem()).custoUnitario(i.getCustoUnitario()).build()).toList();return CompraResponse.builder().id(c.getId()).tipoCompra(c.getTipoCompra()).dataCompra(c.getDataCompra()).fornecedor(c.getFornecedor()).observacao(c.getObservacao()).valorTotal(moeda(c.getValorTotal())).quantidadeItens(itens.size()).criadoEm(c.getCriadoEm()).atualizadoEm(c.getAtualizadoEm()).status(c.getStatus()).itens(itens).build();}
    private BigDecimal quantidade(BigDecimal q,UnidadeMedida u){if(q==null||q.signum()<=0)throw new BusinessException("Quantidade deve ser maior que zero.");if(q.stripTrailingZeros().scale()>3)throw new BusinessException("Quantidade deve ter no máximo três casas decimais.");if(u==UnidadeMedida.UNIDADE&&q.stripTrailingZeros().scale()>0)throw new BusinessException("Quantidade em unidade deve ser inteira.");return q.setScale(3,RoundingMode.UNNECESSARY);}
    private BigDecimal valor(BigDecimal v){if(v==null||v.signum()<=0)throw new BusinessException("Valor pago deve ser maior que zero.");if(v.stripTrailingZeros().scale()>2)throw new BusinessException("Valor pago deve ter no máximo duas casas decimais.");return v.setScale(2,RoundingMode.UNNECESSARY);}
    private void validarPeriodo(LocalDate i,LocalDate f){if(i!=null&&f!=null&&i.isAfter(f))throw new BusinessException("A data inicial não pode ser posterior à data final.");}
    private void validarCategoria(TipoItemEstoque tipo){if(tipo!=null&&tipo==TipoItemEstoque.PREPARACAO_PRODUZIDA)throw new BusinessException("A análise aceita somente Insumos e Produtos de revenda.");}
    private void validarSelecaoItem(TipoItemEstoque tipo,Long id,UnidadeMedida unidade){boolean nenhum=tipo==null&&id==null&&unidade==null,todos=tipo!=null&&id!=null&&unidade!=null;if(!nenhum&&!todos)throw new BusinessException("Selecione um item válido.");validarCategoria(tipo);}
    private OpcaoAnalisePrecoResponse mapearOpcao(CompraRepository.OpcaoItemAnalise r){TipoItemEstoque tipo=TipoItemEstoque.valueOf(r.getTipoItem());UnidadeMedida unidade=UnidadeMedida.valueOf(r.getUnidade());return new OpcaoAnalisePrecoResponse(tipo,r.getReferenciaId(),r.getItemNome(),unidade,tipo.getDescricao(),unidade.getSimbolo());}
    private AnalisePrecosCompraResponse.FornecedorItem mapearAnalise(CompraRepository.AnaliseFornecedor r){
        BigDecimal quantidade=r.getQuantidadeTotal()==null?BigDecimal.ZERO:r.getQuantidadeTotal();
        BigDecimal media=quantidade.signum()==0?BigDecimal.ZERO.setScale(6):r.getValorPago().divide(quantidade,6,RoundingMode.HALF_UP);
        return new AnalisePrecosCompraResponse.FornecedorItem(TipoItemEstoque.valueOf(r.getTipoItem()),r.getReferenciaId(),
                r.getItemNome(),r.getFornecedor(),UnidadeMedida.valueOf(r.getUnidade()),r.getUltimaCompra(),r.getUltimoPreco(),
                r.getMenorPreco(),r.getMaiorPreco(),media,quantidade);
    }
    private AnalisePrecosCompraResponse.ComparativoItem comparar(ChaveAnalise chave,List<AnalisePrecosCompraResponse.FornecedorItem> linhas){
        BigDecimal menor=linhas.stream().map(AnalisePrecosCompraResponse.FornecedorItem::menorPreco).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        BigDecimal maior=linhas.stream().map(AnalisePrecosCompraResponse.FornecedorItem::maiorPreco).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<AnalisePrecosCompraResponse.PrecoFornecedor> melhores=linhas.stream().filter(l->l.menorPreco().compareTo(menor)==0)
                .map(l->new AnalisePrecosCompraResponse.PrecoFornecedor(l.fornecedor(),l.menorPreco())).toList();
        List<AnalisePrecosCompraResponse.PrecoFornecedor> piores=linhas.stream().filter(l->l.maiorPreco().compareTo(maior)==0)
                .map(l->new AnalisePrecosCompraResponse.PrecoFornecedor(l.fornecedor(),l.maiorPreco())).toList();
        return new AnalisePrecosCompraResponse.ComparativoItem(chave.categoria,chave.referenciaId,chave.item,chave.unidade,
                melhores,piores,maior.subtract(menor),linhas.size());
    }
    private long numero(Long valor){return valor==null?0:valor;}
    private String normalizar(String v){return v==null?"":v.trim();}private String normalizarOpcional(String v){String n=normalizar(v);return n.isEmpty()?null:n;}private BigDecimal moeda(BigDecimal v){return(v==null?BigDecimal.ZERO:v).setScale(2,RoundingMode.HALF_UP);}
    private record ChaveAnalise(TipoItemEstoque categoria,Long referenciaId,String item,UnidadeMedida unidade){}
}
