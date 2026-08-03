package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.producao.*;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.ProducaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Controller
@RequestMapping("/producoes")
@RequiredArgsConstructor
public class ProducaoWebController {
    private static final Set<Integer> TAMANHOS = Set.of(10,20,50);
    private final ProducaoService producaoService;

    @GetMapping
    public String listar(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dataInicial,
                         @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dataFinal,
                         @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="10") int tamanho, Model model) {
        int tamanhoSeguro=TAMANHOS.contains(tamanho)?tamanho:10;
        PageRequest pageable=PageRequest.of(Math.max(0,pagina),tamanhoSeguro,Sort.by(Sort.Direction.DESC,"dataProducao"));
        Page<ProducaoResumoResponse> producoes;
        try { producoes=producaoService.listar(dataInicial,dataFinal,pageable); }
        catch(BusinessException e){producoes=Page.empty(pageable);model.addAttribute("mensagemErro",e.getMessage());}
        model.addAttribute("paginaProducoes",producoes);model.addAttribute("producoes",producoes.getContent());
        model.addAttribute("dataInicial",dataInicial);model.addAttribute("dataFinal",dataFinal);model.addAttribute("tamanho",tamanhoSeguro);
        return "producoes/listar";
    }

    @GetMapping("/nova")
    public String nova(Model model){model.addAttribute("producao",ProducaoRequest.builder().dataProducao(LocalDate.now())
            .valorIngredientes(BigDecimal.ZERO).valorEmbalagens(BigDecimal.ZERO).valorGasEnergia(BigDecimal.ZERO).valorOutros(BigDecimal.ZERO).build());
        prepararFormulario(model,false,null);return "producoes/formulario";}

    @PostMapping
    public String salvar(@Valid @ModelAttribute("producao") ProducaoRequest request,BindingResult result,Model model,RedirectAttributes redirect){
        if(result.hasErrors()){prepararFormulario(model,false,null);return "producoes/formulario";}
        try{var salva=producaoService.salvar(request);redirect.addFlashAttribute("mensagemSucesso","Produção cadastrada com sucesso.");return "redirect:/producoes/"+salva.id();}
        catch(BusinessException e){result.reject("producao.invalida",e.getMessage());prepararFormulario(model,false,null);return "producoes/formulario";}}

    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id,Model model){model.addAttribute("resumo",producaoService.buscarResumoPorId(id));return "producoes/detalhes";}

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id,Model model){var p=producaoService.buscarPorId(id);model.addAttribute("producao",ProducaoRequest.builder()
            .dataProducao(p.dataProducao()).valorIngredientes(p.valorIngredientes()).valorEmbalagens(p.valorEmbalagens())
            .valorGasEnergia(p.valorGasEnergia()).valorOutros(p.valorOutros()).observacao(p.observacao()).build());
        prepararFormulario(model,true,id);return "producoes/formulario";}

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id,@Valid @ModelAttribute("producao") ProducaoRequest request,BindingResult result,Model model,RedirectAttributes redirect){
        if(result.hasErrors()){prepararFormulario(model,true,id);return "producoes/formulario";}
        try{producaoService.atualizar(id,request);redirect.addFlashAttribute("mensagemSucesso","Produção atualizada com sucesso.");return "redirect:/producoes/"+id;}
        catch(BusinessException e){result.reject("producao.invalida",e.getMessage());prepararFormulario(model,true,id);return "producoes/formulario";}}

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id,RedirectAttributes redirect){producaoService.excluir(id);redirect.addFlashAttribute("mensagemSucesso","Produção excluída com sucesso.");return "redirect:/producoes";}

    private void prepararFormulario(Model model,boolean edicao,Long id){model.addAttribute("modoEdicao",edicao);model.addAttribute("producaoId",id);
        model.addAttribute("titulo",edicao?"Editar produção":"Nova produção");}
}
