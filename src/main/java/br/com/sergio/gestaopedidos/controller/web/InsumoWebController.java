package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.insumo.*;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.InsumoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.Set;

@Controller
@RequestMapping("/insumos")
@RequiredArgsConstructor
public class InsumoWebController {
    private static final Set<String> CAMPOS_ORDENACAO = Set.of("id", "nome", "unidadeMedida", "ativo", "atualizadoEm");
    private static final Set<Integer> TAMANHOS = Set.of(10, 20, 50);
    private final InsumoService insumoService;

    @ModelAttribute("unidadesMedida")
    public UnidadeMedida[] unidadesMedida() { return UnidadeMedida.values(); }

    @GetMapping
    public String listar(@RequestParam(defaultValue="") String filtro,
                         @RequestParam(required=false) Boolean ativo,
                         @RequestParam(defaultValue="0") int pagina,
                         @RequestParam(defaultValue="10") int tamanho,
                         @RequestParam(defaultValue="nome") String ordenarPor,
                         @RequestParam(defaultValue="asc") String direcao, Model model) {
        String campo = CAMPOS_ORDENACAO.contains(ordenarPor) ? ordenarPor : "nome";
        Sort.Direction sentido = "desc".equalsIgnoreCase(direcao) ? Sort.Direction.DESC : Sort.Direction.ASC;
        int tamanhoSeguro = TAMANHOS.contains(tamanho) ? tamanho : 10;
        String busca = filtro == null ? "" : filtro.trim();
        PageRequest pageable = PageRequest.of(Math.max(0, pagina), tamanhoSeguro, Sort.by(sentido, campo));
        Page<InsumoResponse> resultado = insumoService.listar(busca, ativo, pageable);
        model.addAttribute("paginaInsumos", resultado); model.addAttribute("insumos", resultado.getContent());
        model.addAttribute("filtro", busca); model.addAttribute("ativo", ativo); model.addAttribute("tamanho", tamanhoSeguro);
        model.addAttribute("ordenarPor", campo); model.addAttribute("direcao", sentido.name().toLowerCase());
        model.addAttribute("temFiltros", !busca.isBlank() || ativo != null);
        return "insumos/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("insumo", InsumoRequest.builder().ativo(true).estoqueMinimo(BigDecimal.ZERO).build());
        prepararFormulario(model, false, null); return "insumos/formulario";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("insumo") InsumoRequest request, BindingResult result,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { prepararFormulario(model, false, null); return "insumos/formulario"; }
        try { insumoService.salvar(request); redirect.addFlashAttribute("mensagemSucesso", "Insumo cadastrado com sucesso."); return "redirect:/insumos"; }
        catch (BusinessException e) { result.reject("insumo.invalido", e.getMessage()); prepararFormulario(model, false, null); return "insumos/formulario"; }
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        InsumoResponse i = insumoService.buscarPorId(id);
        model.addAttribute("insumo", InsumoRequest.builder().nome(i.nome()).descricao(i.descricao())
                .unidadeMedida(i.unidadeMedida()).ativo(i.ativo()).estoqueMinimo(i.estoqueMinimo())
                .observacao(i.observacao()).build());
        prepararFormulario(model, true, id); return "insumos/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("insumo") InsumoRequest request,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { prepararFormulario(model, true, id); return "insumos/formulario"; }
        try { insumoService.atualizar(id, request); redirect.addFlashAttribute("mensagemSucesso", "Insumo atualizado com sucesso."); return "redirect:/insumos"; }
        catch (BusinessException e) { result.reject("insumo.invalido", e.getMessage()); prepararFormulario(model, true, id); return "insumos/formulario"; }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) { insumoService.ativar(id); redirect.addFlashAttribute("mensagemSucesso", "Insumo ativado com sucesso."); return "redirect:/insumos"; }
    @PostMapping("/{id}/inativar")
    public String inativar(@PathVariable Long id, RedirectAttributes redirect) { insumoService.inativar(id); redirect.addFlashAttribute("mensagemSucesso", "Insumo inativado com sucesso."); return "redirect:/insumos"; }
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes redirect) { insumoService.excluir(id); redirect.addFlashAttribute("mensagemSucesso", "Insumo excluído com sucesso."); return "redirect:/insumos"; }

    private void prepararFormulario(Model model, boolean edicao, Long id) {
        model.addAttribute("titulo", edicao ? "Editar insumo" : "Novo insumo");
        model.addAttribute("modoEdicao", edicao); model.addAttribute("insumoId", id);
    }
}
