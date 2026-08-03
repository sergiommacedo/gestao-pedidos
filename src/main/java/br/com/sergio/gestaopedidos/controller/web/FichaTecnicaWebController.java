package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.ficha.*;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/fichas-tecnicas")
@RequiredArgsConstructor
public class FichaTecnicaWebController {
    private static final Set<Integer> TAMANHOS = Set.of(10, 20, 50);
    private final FichaTecnicaService fichaService;
    private final ProdutoService produtoService;

    @GetMapping
    public String listar(@RequestParam(defaultValue = "") String produto,
                         @RequestParam(required = false) Boolean ativa,
                         @RequestParam(defaultValue = "") String situacaoCusto,
                         @RequestParam(defaultValue = "0") int pagina,
                         @RequestParam(defaultValue = "10") int tamanho, Model model) {
        int limite = TAMANHOS.contains(tamanho) ? tamanho : 10;
        var resultado = fichaService.listar(produto, ativa, situacaoCusto,
                PageRequest.of(Math.max(0, pagina), limite));
        model.addAttribute("paginaFichas", resultado); model.addAttribute("fichas", resultado.getContent());
        model.addAttribute("produto", produto); model.addAttribute("ativa", ativa);
        model.addAttribute("situacaoCusto", situacaoCusto); model.addAttribute("tamanho", limite);
        model.addAttribute("temFiltros", !produto.isBlank() || ativa != null || !situacaoCusto.isBlank());
        return "fichas-tecnicas/listar";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("ficha", FichaTecnicaRequest.builder().ativa(true).build());
        prepararFormulario(model, false, null, List.of());
        return "fichas-tecnicas/formulario";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("ficha") FichaTecnicaRequest request, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) try {
            var salva = fichaService.salvar(request);
            redirect.addFlashAttribute("mensagemSucesso", "Ficha técnica cadastrada com sucesso.");
            return "redirect:/fichas-tecnicas/" + salva.id();
        } catch (BusinessException e) { result.reject("ficha.invalida", e.getMessage()); }
        prepararFormulario(model, false, null, itensFormulario(request));
        return "fichas-tecnicas/formulario";
    }

    @GetMapping("/{id}")
    public String detalhes(@PathVariable Long id, Model model) {
        model.addAttribute("ficha", fichaService.buscarPorId(id));
        return "fichas-tecnicas/detalhes";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        var ficha = fichaService.buscarPorId(id);
        FichaTecnicaRequest request = FichaTecnicaRequest.builder().produtoId(ficha.produtoId())
                .observacao(ficha.observacao()).ativa(ficha.ativa())
                .itens(ficha.itens().stream().map(i -> ItemFichaTecnicaRequest.builder().id(i.id())
                        .insumoId(i.insumoId()).quantidade(i.quantidade()).build()).toList()).build();
        model.addAttribute("ficha", request);
        prepararFormulario(model, true, id, ficha.itens());
        return "fichas-tecnicas/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("ficha") FichaTecnicaRequest request,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) try {
            fichaService.atualizar(id, request);
            redirect.addFlashAttribute("mensagemSucesso", "Ficha técnica atualizada com sucesso.");
            return "redirect:/fichas-tecnicas/" + id;
        } catch (BusinessException e) { result.reject("ficha.invalida", e.getMessage()); }
        prepararFormulario(model, true, id, itensFormulario(request));
        return "fichas-tecnicas/formulario";
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        fichaService.ativar(id); redirect.addFlashAttribute("mensagemSucesso", "Ficha técnica ativada com sucesso.");
        return "redirect:/fichas-tecnicas";
    }

    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Long id, RedirectAttributes redirect) {
        fichaService.inativar(id); redirect.addFlashAttribute("mensagemSucesso", "Ficha técnica inativada com sucesso.");
        return "redirect:/fichas-tecnicas";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) {
        fichaService.excluir(id); redirect.addFlashAttribute("mensagemSucesso", "Ficha técnica excluída com sucesso.");
        return "redirect:/fichas-tecnicas";
    }

    @GetMapping("/insumos/buscar") @ResponseBody
    public List<InsumoFichaCustoResponse> buscarInsumos(@RequestParam(defaultValue = "") String termo) {
        return fichaService.buscarInsumos(termo);
    }

    @GetMapping("/insumos/{id}/custo") @ResponseBody
    public InsumoFichaCustoResponse custoInsumo(@PathVariable Long id) { return fichaService.buscarCustoInsumo(id); }

    private void prepararFormulario(Model model, boolean edicao, Long id, List<?> itens) {
        model.addAttribute("titulo", edicao ? "Editar ficha técnica" : "Nova ficha técnica");
        model.addAttribute("modoEdicao", edicao); model.addAttribute("fichaId", id);
        var produtos = new ArrayList<>(produtoService.listarProduzidosAtivos());
        if (edicao && id != null) {
            Long produtoId = fichaService.buscarPorId(id).produtoId();
            if (produtos.stream().noneMatch(p -> p.id().equals(produtoId)))
                produtos.add(produtoService.buscarPorId(produtoId));
        }
        produtos.sort(Comparator.comparing(p -> p.nome().toLowerCase()));
        model.addAttribute("produtos", produtos);
        model.addAttribute("itensFormulario", itens);
    }

    private List<ItemFichaTecnicaResponse> itensFormulario(FichaTecnicaRequest request) {
        if (request.getItens() == null) return List.of();
        List<ItemFichaTecnicaResponse> itens = new ArrayList<>();
        for (var item : request.getItens()) try {
            var custo = fichaService.buscarCustoInsumo(item.getInsumoId());
            var estimado = item.getQuantidade() == null ? java.math.BigDecimal.ZERO
                    : item.getQuantidade().multiply(custo.custoMedio()).setScale(2, java.math.RoundingMode.HALF_UP);
            itens.add(ItemFichaTecnicaResponse.builder().id(item.getId()).insumoId(custo.id())
                    .insumoNome(custo.nome()).unidadeMedida(custo.unidade()).quantidade(item.getQuantidade())
                    .estoqueAtual(custo.estoqueAtual()).custoMedioAtual(custo.custoMedio())
                    .custoEstimado(estimado).possuiCusto(custo.possuiCusto()).build());
        } catch (RuntimeException ignored) { /* O erro original permanece no BindingResult. */ }
        return itens;
    }
}
