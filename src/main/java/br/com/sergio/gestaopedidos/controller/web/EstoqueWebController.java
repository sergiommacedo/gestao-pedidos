package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.estoque.*;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.*;
import java.util.Set;

@Controller
@RequestMapping("/estoque")
@RequiredArgsConstructor
public class EstoqueWebController {
    private static final Set<Integer> TAMANHOS = Set.of(10, 20, 50);
    private final EstoqueService estoqueService;
    private final InsumoService insumoService;
    private final ProdutoService produtoService;

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String nome,
                         @RequestParam(required = false) TipoItemEstoque categoria,
                         @RequestParam(defaultValue = "") String situacao,
                         @RequestParam(required = false) Boolean ativo,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho, Model model) {
        int limite = tamanhoValido(tamanho, 10);
        var resultado = estoqueService.listar(nome, categoria, situacao, ativo,
                PageRequest.of(Math.max(0, pagina), limite));
        model.addAttribute("paginaEstoque", resultado);
        model.addAttribute("estoques", resultado.getContent());
        model.addAttribute("indicadores", estoqueService.indicadores());
        model.addAttribute("nome", nome);
        model.addAttribute("categoria", categoria);
        model.addAttribute("situacao", situacao);
        model.addAttribute("ativo", ativo);
        model.addAttribute("tamanho", limite);
        model.addAttribute("categorias", categoriasManuais());
        return "estoque/listar";
    }

    @GetMapping("/entrada")
    public String entrada(@RequestParam(required = false) TipoItemEstoque tipo,
                          @RequestParam(required = false) Long id, Model model) {
        if (!model.containsAttribute("entrada")) model.addAttribute("entrada", EntradaEstoqueRequest.builder()
                .tipoItem(tipo).referenciaId(id).dataMovimentacao(agora()).build());
        prepararFormulario(model);
        return "estoque/entrada";
    }

    @PostMapping("/entrada")
    public String registrarEntrada(@Valid @ModelAttribute("entrada") EntradaEstoqueRequest request,
                                   BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) try {
            estoqueService.registrarEntradaManual(request);
            redirect.addFlashAttribute("mensagemSucesso", "Entrada registrada com sucesso.");
            return "redirect:/estoque";
        } catch (BusinessException e) { result.reject("entrada.invalida", e.getMessage()); }
        prepararFormulario(model);
        return "estoque/entrada";
    }

    @GetMapping("/saida")
    public String saida(@RequestParam(required = false) TipoItemEstoque tipo,
                        @RequestParam(required = false) Long id, Model model) {
        if (!model.containsAttribute("saida")) model.addAttribute("saida", SaidaEstoqueRequest.builder()
                .tipoItem(tipo).referenciaId(id).dataMovimentacao(agora())
                .tipo(TipoMovimentacaoEstoque.SAIDA_CONSUMO_MANUAL).build());
        prepararFormulario(model);
        return "estoque/saida";
    }

    @PostMapping("/saida")
    public String registrarSaida(@Valid @ModelAttribute("saida") SaidaEstoqueRequest request,
                                 BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) try {
            estoqueService.registrarSaidaManual(request);
            redirect.addFlashAttribute("mensagemSucesso", "Saída registrada com sucesso.");
            return "redirect:/estoque";
        } catch (BusinessException e) { result.reject("saida.invalida", e.getMessage()); }
        prepararFormulario(model);
        return "estoque/saida";
    }

    @GetMapping("/movimentacoes")
    public String movimentacoes(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
                                @RequestParam(required = false) TipoItemEstoque categoria,
                                @RequestParam(required = false) Long referenciaId,
                                @RequestParam(required = false) TipoMovimentacaoEstoque tipo,
                                @RequestParam(required = false) Long compraId,
                                @RequestParam(defaultValue = "0") int pagina,
                                @RequestParam(defaultValue = "20") int tamanho, Model model) {
        int limite = tamanhoValido(tamanho, 20);
        var resultado = estoqueService.movimentacoes(dataInicial, dataFinal, categoria, referenciaId, tipo, compraId,
                PageRequest.of(Math.max(0, pagina), limite));
        model.addAttribute("paginaMovimentacoes", resultado);
        model.addAttribute("movimentacoes", resultado.getContent());
        model.addAttribute("dataInicial", dataInicial); model.addAttribute("dataFinal", dataFinal);
        model.addAttribute("categoria", categoria); model.addAttribute("referenciaId", referenciaId);
        model.addAttribute("tipo", tipo); model.addAttribute("compraId", compraId);
        model.addAttribute("tipos", TipoMovimentacaoEstoque.values()); model.addAttribute("tamanho", limite);
        prepararFormulario(model);
        return "estoque/movimentacoes";
    }

    @GetMapping("/{tipo}/{id}")
    public String detalhes(@PathVariable TipoItemEstoque tipo, @PathVariable Long id, Model model) {
        model.addAttribute("detalhes", estoqueService.detalhes(tipo, id));
        return "estoque/detalhes";
    }

    private void prepararFormulario(Model model) {
        model.addAttribute("categorias", categoriasManuais());
        model.addAttribute("insumos", insumoService.buscarAtivosPorNome(""));
        model.addAttribute("produtosRevenda", produtoService.buscarRevendaAtivosPorNome(""));
        model.addAttribute("tiposSaida", new TipoMovimentacaoEstoque[]{TipoMovimentacaoEstoque.SAIDA_CONSUMO_MANUAL,
                TipoMovimentacaoEstoque.SAIDA_PERDA, TipoMovimentacaoEstoque.SAIDA_AJUSTE});
    }

    private int tamanhoValido(int tamanho, int padrao) { return TAMANHOS.contains(tamanho) ? tamanho : padrao; }
    private TipoItemEstoque[] categoriasManuais(){return new TipoItemEstoque[]{TipoItemEstoque.INSUMO,TipoItemEstoque.PRODUTO_REVENDA};}
    private LocalDateTime agora() { return LocalDateTime.now().withSecond(0).withNano(0); }
}
