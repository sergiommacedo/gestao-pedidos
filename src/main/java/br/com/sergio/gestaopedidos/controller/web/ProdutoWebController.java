package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.produto.ProdutoRequest;
import br.com.sergio.gestaopedidos.dto.produto.ProdutoResponse;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.enums.TipoProduto;
import br.com.sergio.gestaopedidos.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/produtos")
@RequiredArgsConstructor
public class ProdutoWebController {

    private static final Set<String> CAMPOS_ORDENACAO =
            Set.of("id", "nome", "preco", "unidadeVenda", "tipoProduto", "vendavel", "ativo");

    private final ProdutoService produtoService;

    @ModelAttribute("unidadesVenda")
    public UnidadeVenda[] unidadesVenda() {
        return UnidadeVenda.values();
    }

    @ModelAttribute("tiposProduto")
    public TipoProduto[] tiposProduto() {
        return TipoProduto.values();
    }

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(defaultValue = "id") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao,
            Model model
    ) {
        String campoOrdenacao = validarCampoOrdenacao(ordenarPor);
        Sort.Direction direcaoOrdenacao = converterDirecao(direcao);
        int paginaValida = Math.max(pagina, 0);
        int tamanhoValido = validarTamanhoPagina(tamanho);
        String filtroTratado = filtro == null ? "" : filtro.trim();

        PageRequest pageable = PageRequest.of(
                paginaValida,
                tamanhoValido,
                Sort.by(direcaoOrdenacao, campoOrdenacao)
        );

        Page<ProdutoResponse> paginaProdutos =
                produtoService.listarPaginado(filtroTratado, pageable);

        model.addAttribute("paginaProdutos", paginaProdutos);
        model.addAttribute("produtos", paginaProdutos.getContent());
        model.addAttribute("filtro", filtroTratado);
        model.addAttribute("ordenarPor", campoOrdenacao);
        model.addAttribute("direcao", direcaoOrdenacao.name().toLowerCase());
        model.addAttribute("tamanho", tamanhoValido);

        return "produtos/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("produto", ProdutoRequest.builder()
                .ativo(true)
                .tipoProduto(TipoProduto.PRODUZIDO)
                .vendavel(true)
                .permiteAcompanhamento(false)
                .estoqueMinimo(java.math.BigDecimal.ZERO)
                .build());
        prepararFormulario(model, "Novo Produto", false, null);
        return "produtos/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        ProdutoResponse produto = produtoService.buscarPorId(id);
        ProdutoRequest request = ProdutoRequest.builder()
                .nome(produto.nome())
                .descricao(produto.descricao())
                .preco(produto.preco())
                .unidadeVenda(produto.unidadeVenda())
                .permiteAcompanhamento(produto.permiteAcompanhamento())
                .tipoProduto(produto.tipoProduto())
                .vendavel(produto.vendavel())
                .ativo(produto.ativo())
                .estoqueMinimo(produto.estoqueMinimo())
                .build();

        model.addAttribute("produto", request);
        prepararFormulario(model, "Editar Produto", true, id);
        return "produtos/formulario";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("produto") ProdutoRequest produto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararFormulario(model, "Novo Produto", false, null);
            return "produtos/formulario";
        }

        produtoService.salvar(produto);
        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Produto cadastrado com sucesso."
        );
        return "redirect:/produtos";
    }

    @PostMapping("/{id}")
    public String atualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("produto") ProdutoRequest produto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararFormulario(model, "Editar Produto", true, id);
            return "produtos/formulario";
        }

        produtoService.atualizar(id, produto);
        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Produto atualizado com sucesso."
        );
        return "redirect:/produtos";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        produtoService.excluir(id);
        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Produto excluído com sucesso."
        );
        return "redirect:/produtos";
    }

    private void prepararFormulario(
            Model model,
            String titulo,
            boolean modoEdicao,
            Long produtoId
    ) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("modoEdicao", modoEdicao);
        if (produtoId != null) {
            model.addAttribute("produtoId", produtoId);
        }
    }

    private String validarCampoOrdenacao(String ordenarPor) {
        return CAMPOS_ORDENACAO.contains(ordenarPor) ? ordenarPor : "id";
    }

    private Sort.Direction converterDirecao(String direcao) {
        return "asc".equalsIgnoreCase(direcao)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
    }

    private int validarTamanhoPagina(int tamanho) {
        return tamanho == 10 || tamanho == 20 || tamanho == 50 ? tamanho : 10;
    }
}
