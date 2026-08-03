package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.compra.*;
import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import br.com.sergio.gestaopedidos.exception.*;
import br.com.sergio.gestaopedidos.repository.CompraInsumoRepository;
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
public class CompraInsumoService {
    private final CompraInsumoRepository compraRepository;
    private final InsumoService insumoService;
    private final EstoqueService estoqueService;

    @Transactional(readOnly=true)
    public Page<CompraInsumoResponse> listar(LocalDate inicio,LocalDate fim,String fornecedor,Pageable pageable){
        validarPeriodo(inicio,fim);
        return compraRepository.buscar(inicio,fim,normalizar(fornecedor),pageable).map(r->CompraInsumoResponse.builder()
                .id(r.getId()).dataCompra(r.getDataCompra()).fornecedor(r.getFornecedor()).valorTotal(moeda(r.getValorTotal()))
                .quantidadeItens(r.getQuantidadeItens().intValue()).atualizadoEm(r.getAtualizadoEm()).status(r.getStatus()).itens(List.of()).build());
    }

    @Transactional(readOnly=true) public CompraInsumoResponse buscarPorId(Long id){return mapear(buscarEntidade(id));}

    public CompraInsumoResponse salvar(CompraInsumoRequest request){
        validarCabecalho(request); CompraInsumo compra=new CompraInsumo(); aplicarCabecalho(compra,request);
        validarIdsEDuplicidade(request.getItens(),Set.of());
        for(ItemCompraInsumoRequest itemRequest:request.getItens()){
            Insumo insumo=buscarInsumoAtivo(itemRequest.getInsumoId());
            ItemCompraInsumo item=new ItemCompraInsumo(); aplicarItem(item,itemRequest,insumo,insumo.getUnidadeMedida()); compra.adicionarItem(item);
        }
        compra.recalcularTotal(); compra=compraRepository.saveAndFlush(compra);estoqueService.processarCompra(compra);return mapear(compra);
    }

    public CompraInsumoResponse atualizar(Long id,CompraInsumoRequest request){
        validarCabecalho(request); CompraInsumo compra=buscarEntidade(id);
        if(estoqueService.compraMovimentada(id)){validarFinanceiroImutavel(compra,request);compra.setFornecedor(normalizarOpcional(request.getFornecedor()));compra.setObservacao(normalizarOpcional(request.getObservacao()));return mapear(compraRepository.save(compra));}
        aplicarCabecalho(compra,request);
        Map<Long,ItemCompraInsumo> existentes=compra.getItens().stream().collect(Collectors.toMap(ItemCompraInsumo::getId,Function.identity()));
        validarIdsEDuplicidade(request.getItens(),existentes.keySet()); Set<Long> mantidos=new HashSet<>();
        for(ItemCompraInsumoRequest recebido:request.getItens()){
            if(recebido.getId()==null){Insumo insumo=buscarInsumoAtivo(recebido.getInsumoId());ItemCompraInsumo novo=new ItemCompraInsumo();aplicarItem(novo,recebido,insumo,insumo.getUnidadeMedida());compra.adicionarItem(novo);continue;}
            ItemCompraInsumo item=existentes.get(recebido.getId()); mantidos.add(item.getId());
            boolean mesmoInsumo=Objects.equals(item.getInsumo().getId(),recebido.getInsumoId());
            Insumo insumo=mesmoInsumo?item.getInsumo():buscarInsumoAtivo(recebido.getInsumoId());
            UnidadeMedida unidade=mesmoInsumo?item.getUnidadeMedida():insumo.getUnidadeMedida();
            BigDecimal quantidade=quantidade(recebido.getQuantidade(),unidade); BigDecimal valor=valor(recebido.getValorTotalItem());
            boolean alterou=item.getQuantidade().compareTo(quantidade)!=0||item.getValorTotalItem().compareTo(valor)!=0||!mesmoInsumo;
            item.setInsumo(insumo);item.setUnidadeMedida(unidade);item.setQuantidade(quantidade);item.setValorTotalItem(valor);
            if(alterou)item.setCustoUnitario(calcularCustoUnitario(valor,quantidade));
        }
        for(Iterator<ItemCompraInsumo> it=compra.getItens().iterator();it.hasNext();){ItemCompraInsumo item=it.next();if(item.getId()!=null&&!mantidos.contains(item.getId())){it.remove();item.setCompra(null);}}
        compra.recalcularTotal(); return mapear(compraRepository.save(compra));
    }

    public void excluir(Long id){if(estoqueService.compraMovimentada(id))throw new BusinessException("Compra com movimentação de estoque não pode ser excluída. Utilize o estorno.");compraRepository.delete(buscarEntidade(id));}
    public void estornar(Long id){CompraInsumo compra=buscarEntidade(id);estoqueService.estornarCompra(compra);compraRepository.save(compra);}
    @Transactional(readOnly=true) public boolean compraMovimentada(Long id){return estoqueService.compraMovimentada(id);}
    private void validarFinanceiroImutavel(CompraInsumo c,CompraInsumoRequest r){if(!Objects.equals(c.getDataCompra(),r.getDataCompra())||r.getItens()==null||c.getItens().size()!=r.getItens().size())imutavel();Map<Long,ItemCompraInsumoRequest> enviados=r.getItens().stream().filter(i->i.getId()!=null).collect(Collectors.toMap(ItemCompraInsumoRequest::getId,Function.identity(),(a,b)->{imutavel();return a;}));for(var atual:c.getItens()){var recebido=enviados.get(atual.getId());if(recebido==null||!Objects.equals(recebido.getInsumoId(),atual.getInsumo().getId())||atual.getQuantidade().compareTo(recebido.getQuantidade())!=0||atual.getValorTotalItem().compareTo(recebido.getValorTotalItem())!=0)imutavel();}}
    private void imutavel(){throw new BusinessException("Esta compra já movimentou o estoque. Somente fornecedor e observação podem ser alterados.");}

    private void validarCabecalho(CompraInsumoRequest r){if(r.getDataCompra()==null)throw new BusinessException("Data da compra é obrigatória.");if(r.getItens()==null||r.getItens().isEmpty())throw new BusinessException("Adicione ao menos um item à compra.");}
    private void validarIdsEDuplicidade(List<ItemCompraInsumoRequest> itens,Set<Long> idsExistentes){Set<Long> ids=new HashSet<>();Set<Long> insumos=new HashSet<>();for(var i:itens){if(i.getId()!=null&&(!ids.add(i.getId())||!idsExistentes.contains(i.getId())))throw new BusinessException("Item informado não pertence a esta compra.");if(i.getInsumoId()==null)throw new BusinessException("Selecione o insumo.");if(!insumos.add(i.getInsumoId()))throw new BusinessException("Este insumo já foi adicionado à compra.");}}
    private Insumo buscarInsumoAtivo(Long id){Insumo i=insumoService.buscarEntidade(id);if(!Boolean.TRUE.equals(i.getAtivo()))throw new BusinessException("Este insumo não está disponível para novas compras.");return i;}
    private void aplicarCabecalho(CompraInsumo c,CompraInsumoRequest r){c.setDataCompra(r.getDataCompra());c.setFornecedor(normalizarOpcional(r.getFornecedor()));c.setObservacao(normalizarOpcional(r.getObservacao()));}
    private void aplicarItem(ItemCompraInsumo item,ItemCompraInsumoRequest r,Insumo insumo,UnidadeMedida unidade){BigDecimal q=quantidade(r.getQuantidade(),unidade),v=valor(r.getValorTotalItem());item.setInsumo(insumo);item.setUnidadeMedida(unidade);item.setQuantidade(q);item.setValorTotalItem(v);item.setCustoUnitario(calcularCustoUnitario(v,q));}
    private BigDecimal quantidade(BigDecimal q,UnidadeMedida unidade){if(q==null||q.signum()<=0)throw new BusinessException("Quantidade deve ser maior que zero.");if(q.stripTrailingZeros().scale()>3)throw new BusinessException("Quantidade deve ter no máximo três casas decimais.");if(unidade==UnidadeMedida.UNIDADE&&q.stripTrailingZeros().scale()>0)throw new BusinessException("Quantidade em unidade deve ser um número inteiro.");return q.setScale(3,RoundingMode.UNNECESSARY);}
    private BigDecimal valor(BigDecimal v){if(v==null||v.signum()<=0)throw new BusinessException("Valor pago deve ser maior que zero.");if(v.stripTrailingZeros().scale()>2)throw new BusinessException("Valor pago deve ter no máximo duas casas decimais.");return v.setScale(2,RoundingMode.UNNECESSARY);}
    private BigDecimal calcularCustoUnitario(BigDecimal valor,BigDecimal quantidade){return valor.divide(quantidade,6,RoundingMode.HALF_UP);}
    private CompraInsumo buscarEntidade(Long id){return compraRepository.buscarDetalhada(id).orElseThrow(()->new ResourceNotFoundException("Compra de insumos não encontrada."));}
    private CompraInsumoResponse mapear(CompraInsumo c){List<ItemCompraInsumoResponse> itens=c.getItens().stream().map(i->ItemCompraInsumoResponse.builder().id(i.getId()).insumoId(i.getInsumo().getId()).insumoNome(i.getInsumo().getNome()).unidadeMedida(i.getUnidadeMedida()).quantidade(i.getQuantidade()).valorTotalItem(i.getValorTotalItem()).custoUnitario(i.getCustoUnitario()).build()).toList();return CompraInsumoResponse.builder().id(c.getId()).dataCompra(c.getDataCompra()).fornecedor(c.getFornecedor()).observacao(c.getObservacao()).valorTotal(moeda(c.getValorTotal())).quantidadeItens(itens.size()).criadoEm(c.getCriadoEm()).atualizadoEm(c.getAtualizadoEm()).status(c.getStatus()).itens(itens).build();}
    private void validarPeriodo(LocalDate i,LocalDate f){if(i!=null&&f!=null&&i.isAfter(f))throw new BusinessException("A data inicial não pode ser posterior à data final.");}
    private String normalizar(String v){return v==null?"":v.trim();}private String normalizarOpcional(String v){String n=normalizar(v);return n.isEmpty()?null:n;}private BigDecimal moeda(BigDecimal v){return(v==null?BigDecimal.ZERO:v).setScale(2,RoundingMode.HALF_UP);}
}
