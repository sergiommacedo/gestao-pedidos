package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.compra.*;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.*;
import java.time.LocalDate;
import java.util.*;

@Controller @RequestMapping("/compras") @RequiredArgsConstructor
public class CompraWebController {
    private static final Set<String>CAMPOS=Set.of("id","dataCompra","fornecedor","valorTotal","atualizadoEm","tipoCompra");
    private static final Set<Integer>TAMANHOS=Set.of(10,20,50);
    private final CompraService compraService;private final InsumoService insumoService;private final ProdutoService produtoService;

    @GetMapping public String listar(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate dataInicial,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate dataFinal,@RequestParam(defaultValue="")String fornecedor,@RequestParam(required=false)TipoCompra tipoCompra,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="10")int tamanho,@RequestParam(defaultValue="dataCompra")String ordenarPor,@RequestParam(defaultValue="desc")String direcao,Model model){
        String campo=CAMPOS.contains(ordenarPor)?ordenarPor:"dataCompra";Sort.Direction sentido="asc".equalsIgnoreCase(direcao)?Sort.Direction.ASC:Sort.Direction.DESC;int limite=TAMANHOS.contains(tamanho)?tamanho:10;Sort sort=Sort.by(sentido,campo);if("dataCompra".equals(campo))sort=sort.and(Sort.by(sentido,"id"));PageRequest pageable=PageRequest.of(Math.max(0,pagina),limite,sort);
        try{var p=compraService.listar(dataInicial,dataFinal,fornecedor,tipoCompra,pageable);model.addAttribute("paginaCompras",p);model.addAttribute("compras",p.getContent());}catch(BusinessException e){model.addAttribute("paginaCompras",Page.empty(pageable));model.addAttribute("compras",List.of());model.addAttribute("mensagemErro",e.getMessage());}
        model.addAttribute("dataInicial",dataInicial);model.addAttribute("dataFinal",dataFinal);model.addAttribute("fornecedor",fornecedor);model.addAttribute("tipoCompra",tipoCompra);model.addAttribute("tiposCompra",TipoCompra.values());model.addAttribute("tamanho",limite);model.addAttribute("ordenarPor",campo);model.addAttribute("direcao",sentido.name().toLowerCase());return"compras/listar";
    }
    @GetMapping("/nova")public String nova(Model model){model.addAttribute("compra",CompraRequest.builder().dataCompra(LocalDate.now()).build());preparar(model,false,null,List.of(),false);return"compras/formulario";}
    @PostMapping public String salvar(@Valid@ModelAttribute("compra")CompraRequest request,BindingResult result,Model model,RedirectAttributes redirect){if(result.hasErrors()){preparar(model,false,null,itensFormulario(request),false);return"compras/formulario";}try{var c=compraService.salvar(request);redirect.addFlashAttribute("mensagemSucesso","Compra cadastrada com sucesso.");return"redirect:/compras/"+c.id();}catch(BusinessException e){result.reject("compra.invalida",e.getMessage());preparar(model,false,null,itensFormulario(request),false);return"compras/formulario";}}
    @GetMapping("/{id}")public String detalhes(@PathVariable Long id,Model model){model.addAttribute("compra",compraService.buscarPorId(id));model.addAttribute("financeiroBloqueado",compraService.compraMovimentada(id));return"compras/detalhes";}
    @GetMapping("/{id}/editar")public String editar(@PathVariable Long id,Model model){var c=compraService.buscarPorId(id);boolean bloqueada=compraService.compraMovimentada(id);model.addAttribute("compra",request(c));preparar(model,true,id,c.itens(),bloqueada);return"compras/formulario";}
    @PostMapping("/{id}")public String atualizar(@PathVariable Long id,@Valid@ModelAttribute("compra")CompraRequest request,BindingResult result,Model model,RedirectAttributes redirect){boolean bloqueada=compraService.compraMovimentada(id);if(result.hasErrors()){preparar(model,true,id,itensFormulario(request),bloqueada);return"compras/formulario";}try{compraService.atualizar(id,request);redirect.addFlashAttribute("mensagemSucesso","Compra atualizada com sucesso.");return"redirect:/compras/"+id;}catch(BusinessException e){result.reject("compra.invalida",e.getMessage());preparar(model,true,id,itensFormulario(request),bloqueada);return"compras/formulario";}}
    @PostMapping("/{id}/estornar")public String estornar(@PathVariable Long id,RedirectAttributes redirect){try{compraService.estornar(id);redirect.addFlashAttribute("mensagemSucesso","Compra estornada com sucesso.");}catch(BusinessException e){redirect.addFlashAttribute("mensagemErro",e.getMessage());}return"redirect:/compras/"+id;}
    @GetMapping("/itens/buscar")@ResponseBody public List<ItemBuscaCompraResponse> buscarItens(@RequestParam(defaultValue="")String termo){
        List<ItemBuscaCompraResponse> itens=new ArrayList<>();
        insumoService.buscarAtivosPorNome(termo).forEach(i->itens.add(itemBusca(i.id(),i.nome(),TipoItemEstoque.INSUMO,i.unidadeMedida())));
        produtoService.buscarRevendaAtivosPorNome(termo).forEach(p->itens.add(itemBusca(p.id(),p.nome(),TipoItemEstoque.PRODUTO_REVENDA,p.unidadeVenda()==UnidadeVenda.UNIDADE?UnidadeMedida.UNIDADE:UnidadeMedida.QUILOGRAMA)));
        return itens.stream().sorted(Comparator.comparing(ItemBuscaCompraResponse::nome,String.CASE_INSENSITIVE_ORDER).thenComparing(ItemBuscaCompraResponse::tipoItem)).limit(20).toList();
    }

    private ItemBuscaCompraResponse itemBusca(Long id,String nome,TipoItemEstoque tipo,UnidadeMedida unidade){return new ItemBuscaCompraResponse(id,nome,tipo,tipo.getDescricao(),unidade,unidade.getDescricao(),unidade.getSimbolo());}
    private CompraRequest request(CompraResponse c){return CompraRequest.builder().dataCompra(c.dataCompra()).fornecedor(c.fornecedor()).observacao(c.observacao()).itens(c.itens().stream().map(i->ItemCompraRequest.builder().id(i.id()).tipoItem(i.tipoItem()).referenciaId(i.referenciaId()).quantidade(i.quantidade()).valorTotalItem(i.valorTotalItem()).build()).toList()).build();}
    private List<ItemCompraResponse> itensFormulario(CompraRequest r){if(r.getItens()==null)return List.of();List<ItemCompraResponse> itens=new ArrayList<>();for(ItemCompraRequest i:r.getItens()){if(i.getTipoItem()==null||i.getReferenciaId()==null)continue;try{String nome;UnidadeMedida unidade;if(i.getTipoItem()==TipoItemEstoque.INSUMO){var ref=insumoService.buscarPorId(i.getReferenciaId());nome=ref.nome();unidade=ref.unidadeMedida();}else if(i.getTipoItem()==TipoItemEstoque.PRODUTO_REVENDA){var ref=produtoService.buscarPorId(i.getReferenciaId());nome=ref.nome();unidade=ref.unidadeVenda()==UnidadeVenda.UNIDADE?UnidadeMedida.UNIDADE:UnidadeMedida.QUILOGRAMA;}else continue;BigDecimal custo=i.getQuantidade()!=null&&i.getQuantidade().signum()>0&&i.getValorTotalItem()!=null?i.getValorTotalItem().divide(i.getQuantidade(),6,RoundingMode.HALF_UP):BigDecimal.ZERO;itens.add(ItemCompraResponse.builder().id(i.getId()).referenciaId(i.getReferenciaId()).tipoItem(i.getTipoItem()).nomeHistorico(nome).categoria(i.getTipoItem().getDescricao()).unidadeHistorica(unidade).quantidade(i.getQuantidade()).valorTotalItem(i.getValorTotalItem()).custoUnitario(custo).build());}catch(RuntimeException ignored){}}return itens;}
    private void preparar(Model model,boolean edicao,Long id,List<ItemCompraResponse> itens,boolean bloqueada){model.addAttribute("titulo",edicao?"Editar compra":"Nova compra");model.addAttribute("modoEdicao",edicao);model.addAttribute("compraId",id);model.addAttribute("itensFormulario",itens);model.addAttribute("financeiroBloqueado",bloqueada);}
}
