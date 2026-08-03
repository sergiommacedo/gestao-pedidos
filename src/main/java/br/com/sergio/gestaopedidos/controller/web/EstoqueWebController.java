package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.estoque.*;
import br.com.sergio.gestaopedidos.enums.TipoMovimentacaoEstoque;
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
import java.time.*;
import java.util.Set;

@Controller @RequestMapping("/estoque") @RequiredArgsConstructor
public class EstoqueWebController {
    private static final Set<Integer> TAMANHOS = Set.of(10, 20, 50);
    private final EstoqueService estoqueService;
    private final InsumoService insumoService;

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String nome, @RequestParam(defaultValue = "") String situacao,
                         @RequestParam(required = false) Boolean ativo, @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho, Model model) {
        int limite = TAMANHOS.contains(tamanho) ? tamanho : 10;
        var p = estoqueService.listar(nome, situacao, ativo, PageRequest.of(Math.max(0, pagina), limite, Sort.by("insumoNome")));
        model.addAttribute("paginaEstoque", p); model.addAttribute("estoques", p.getContent());
        model.addAttribute("indicadores", estoqueService.indicadores()); model.addAttribute("nome", nome);
        model.addAttribute("situacao", situacao); model.addAttribute("ativo", ativo); model.addAttribute("tamanho", limite);
        return "estoque/listar";
    }

    @GetMapping("/entrada") public String entrada(@RequestParam(required=false) Long insumoId, Model model) { if (!model.containsAttribute("entrada")) model.addAttribute("entrada", EntradaEstoqueRequest.builder().insumoId(insumoId).dataMovimentacao(LocalDateTime.now().withSecond(0).withNano(0)).build()); prepararFormulario(model); return "estoque/entrada"; }
    @PostMapping("/entrada") public String registrarEntrada(@Valid @ModelAttribute("entrada") EntradaEstoqueRequest request, BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) try { estoqueService.registrarEntradaManual(request); redirect.addFlashAttribute("mensagemSucesso", "Entrada registrada com sucesso."); return "redirect:/estoque"; } catch (BusinessException e) { result.reject("entrada.invalida", e.getMessage()); }
        prepararFormulario(model); return "estoque/entrada";
    }
    @GetMapping("/saida") public String saida(@RequestParam(required=false) Long insumoId, Model model) { if (!model.containsAttribute("saida")) model.addAttribute("saida", SaidaEstoqueRequest.builder().insumoId(insumoId).dataMovimentacao(LocalDateTime.now().withSecond(0).withNano(0)).tipo(TipoMovimentacaoEstoque.SAIDA_CONSUMO_MANUAL).build()); prepararFormulario(model); return "estoque/saida"; }
    @PostMapping("/saida") public String registrarSaida(@Valid @ModelAttribute("saida") SaidaEstoqueRequest request, BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) try { estoqueService.registrarSaidaManual(request); redirect.addFlashAttribute("mensagemSucesso", "Saída registrada com sucesso."); return "redirect:/estoque"; } catch (BusinessException e) { result.reject("saida.invalida", e.getMessage()); }
        prepararFormulario(model); return "estoque/saida";
    }
    @GetMapping("/movimentacoes") public String movimentacoes(@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dataInicial, @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dataFinal, @RequestParam(required=false) Long insumoId, @RequestParam(required=false) TipoMovimentacaoEstoque tipo, @RequestParam(required=false) Long compraId, @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho, Model model) {
        int limite=TAMANHOS.contains(tamanho)?tamanho:20; var p=estoqueService.movimentacoes(dataInicial,dataFinal,insumoId,tipo,compraId,PageRequest.of(Math.max(0,pagina),limite,Sort.by(Sort.Order.desc("dataMovimentacao"),Sort.Order.desc("id"))));
        model.addAttribute("paginaMovimentacoes",p); model.addAttribute("movimentacoes",p.getContent()); model.addAttribute("dataInicial",dataInicial);model.addAttribute("dataFinal",dataFinal);model.addAttribute("insumoId",insumoId);model.addAttribute("tipo",tipo);model.addAttribute("compraId",compraId);model.addAttribute("tipos",TipoMovimentacaoEstoque.values());model.addAttribute("tamanho",limite);prepararFormulario(model);return "estoque/movimentacoes";
    }
    @GetMapping("/insumos/{id}") public String detalhes(@PathVariable Long id, Model model) { model.addAttribute("detalhes", estoqueService.detalhes(id)); return "estoque/detalhes"; }
    private void prepararFormulario(Model model) { model.addAttribute("insumos", insumoService.buscarAtivosPorNome("")); model.addAttribute("tiposSaida", new TipoMovimentacaoEstoque[]{TipoMovimentacaoEstoque.SAIDA_CONSUMO_MANUAL, TipoMovimentacaoEstoque.SAIDA_PERDA, TipoMovimentacaoEstoque.SAIDA_AJUSTE}); }
}
