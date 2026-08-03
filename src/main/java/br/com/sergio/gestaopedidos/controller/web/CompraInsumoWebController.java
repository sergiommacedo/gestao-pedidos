package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.compra.*;
import br.com.sergio.gestaopedidos.dto.insumo.InsumoResponse;
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

@Controller @RequestMapping("/compras-insumos") @RequiredArgsConstructor
public class CompraInsumoWebController {
    private static final Set<String>CAMPOS=Set.of("id","dataCompra","fornecedor","valorTotal","atualizadoEm");
    private static final Set<Integer>TAMANHOS=Set.of(10,20,50);
    private final CompraInsumoService compraService;private final InsumoService insumoService;

    @GetMapping public String listar(@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate dataInicial,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate dataFinal,@RequestParam(defaultValue="")String fornecedor,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="10")int tamanho,@RequestParam(defaultValue="dataCompra")String ordenarPor,@RequestParam(defaultValue="desc")String direcao,Model model){
        String campo=CAMPOS.contains(ordenarPor)?ordenarPor:"dataCompra";Sort.Direction sentido="asc".equalsIgnoreCase(direcao)?Sort.Direction.ASC:Sort.Direction.DESC;int limite=TAMANHOS.contains(tamanho)?tamanho:10;
        Sort sort=Sort.by(sentido,campo);if("dataCompra".equals(campo))sort=sort.and(Sort.by(sentido,"id"));PageRequest pageable=PageRequest.of(Math.max(0,pagina),limite,sort);
        try{var p=compraService.listar(dataInicial,dataFinal,fornecedor,pageable);model.addAttribute("paginaCompras",p);model.addAttribute("compras",p.getContent());}
        catch(BusinessException e){model.addAttribute("paginaCompras",Page.empty(pageable));model.addAttribute("compras",List.of());model.addAttribute("mensagemErro",e.getMessage());}
        model.addAttribute("dataInicial",dataInicial);model.addAttribute("dataFinal",dataFinal);model.addAttribute("fornecedor",fornecedor);model.addAttribute("tamanho",limite);model.addAttribute("ordenarPor",campo);model.addAttribute("direcao",sentido.name().toLowerCase());return"compras-insumos/listar";}

    @GetMapping("/nova") public String nova(Model model){model.addAttribute("compra",CompraInsumoRequest.builder().dataCompra(LocalDate.now()).build());preparar(model,false,null,List.of());return"compras-insumos/formulario";}
    @PostMapping public String salvar(@Valid@ModelAttribute("compra")CompraInsumoRequest request,BindingResult result,Model model,RedirectAttributes redirect){if(result.hasErrors()){preparar(model,false,null,itensFormulario(request));return"compras-insumos/formulario";}try{var c=compraService.salvar(request);redirect.addFlashAttribute("mensagemSucesso","Compra de insumos cadastrada com sucesso.");return"redirect:/compras-insumos/"+c.id();}catch(BusinessException e){result.reject("compra.invalida",e.getMessage());preparar(model,false,null,itensFormulario(request));return"compras-insumos/formulario";}}
    @GetMapping("/{id}")public String detalhes(@PathVariable Long id,Model model){model.addAttribute("compra",compraService.buscarPorId(id));model.addAttribute("financeiroBloqueado",compraService.compraMovimentada(id));return"compras-insumos/detalhes";}
    @GetMapping("/{id}/editar")public String editar(@PathVariable Long id,Model model){var c=compraService.buscarPorId(id);model.addAttribute("compra",request(c));model.addAttribute("financeiroBloqueado",compraService.compraMovimentada(id));preparar(model,true,id,c.itens());return"compras-insumos/formulario";}
    @PostMapping("/{id}")public String atualizar(@PathVariable Long id,@Valid@ModelAttribute("compra")CompraInsumoRequest request,BindingResult result,Model model,RedirectAttributes redirect){if(result.hasErrors()){preparar(model,true,id,itensFormulario(request));return"compras-insumos/formulario";}try{compraService.atualizar(id,request);redirect.addFlashAttribute("mensagemSucesso","Compra de insumos atualizada com sucesso.");return"redirect:/compras-insumos/"+id;}catch(BusinessException e){result.reject("compra.invalida",e.getMessage());preparar(model,true,id,itensFormulario(request));return"compras-insumos/formulario";}}
    @PostMapping("/{id}/excluir")public String excluir(@PathVariable Long id,RedirectAttributes redirect){compraService.excluir(id);redirect.addFlashAttribute("mensagemSucesso","Compra de insumos excluída com sucesso.");return"redirect:/compras-insumos";}
    @PostMapping("/{id}/estornar")public String estornar(@PathVariable Long id,RedirectAttributes redirect){try{compraService.estornar(id);redirect.addFlashAttribute("mensagemSucesso","Compra estornada com sucesso.");}catch(BusinessException e){redirect.addFlashAttribute("mensagemErro",e.getMessage());}return"redirect:/compras-insumos/"+id;}
    @GetMapping("/insumos/buscar") @ResponseBody
    public List<Map<String,Object>> buscarInsumos(@RequestParam(defaultValue="")String termo){
        return insumoService.buscarAtivosPorNome(termo).stream().map(i->{Map<String,Object> item=new LinkedHashMap<>();item.put("id",i.id());item.put("nome",i.nome());item.put("unidade",i.unidadeMedida().name());item.put("unidadeDescricao",i.unidadeMedida().getDescricao());item.put("simbolo",i.unidadeMedida().getSimbolo());return item;}).toList();
    }

    private CompraInsumoRequest request(CompraInsumoResponse c){return CompraInsumoRequest.builder().dataCompra(c.dataCompra()).fornecedor(c.fornecedor()).observacao(c.observacao()).itens(c.itens().stream().map(i->ItemCompraInsumoRequest.builder().id(i.id()).insumoId(i.insumoId()).quantidade(i.quantidade()).valorTotalItem(i.valorTotalItem()).build()).toList()).build();}
    private List<ItemCompraInsumoResponse> itensFormulario(CompraInsumoRequest r){if(r.getItens()==null)return List.of();List<ItemCompraInsumoResponse> itens=new ArrayList<>();for(var i:r.getItens()){if(i.getInsumoId()==null)continue;try{var insumo=insumoService.buscarPorId(i.getInsumoId());BigDecimal custo=i.getQuantidade()!=null&&i.getQuantidade().signum()>0&&i.getValorTotalItem()!=null?i.getValorTotalItem().divide(i.getQuantidade(),6,RoundingMode.HALF_UP):BigDecimal.ZERO;itens.add(ItemCompraInsumoResponse.builder().id(i.getId()).insumoId(insumo.id()).insumoNome(insumo.nome()).unidadeMedida(insumo.unidadeMedida()).quantidade(i.getQuantidade()).valorTotalItem(i.getValorTotalItem()).custoUnitario(custo).build());}catch(RuntimeException ignored){}}return itens;}
    private void preparar(Model model,boolean edicao,Long id,List<ItemCompraInsumoResponse> itens){model.addAttribute("titulo",edicao?"Editar compra de insumos":"Nova compra de insumos");model.addAttribute("modoEdicao",edicao);model.addAttribute("compraId",id);model.addAttribute("itensFormulario",itens);}
}
