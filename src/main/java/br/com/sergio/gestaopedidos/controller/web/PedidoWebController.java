package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.cliente.ClienteRequest;
import br.com.sergio.gestaopedidos.dto.cliente.ClienteResponse;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.dto.pedido.ItemPedidoRequest;
import br.com.sergio.gestaopedidos.dto.produto.ProdutoResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import br.com.sergio.gestaopedidos.service.ClienteService;
import br.com.sergio.gestaopedidos.service.PedidoService;
import br.com.sergio.gestaopedidos.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoWebController {

    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "id",
            "dataAgendada",
            "cliente.nome",
            "status",
            "valorTotal"
    );

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;

    @ModelAttribute("statusPedidos")
    public StatusPedido[] statusPedidos() {
        return StatusPedido.values();
    }

    @ModelAttribute("formasPagamento")
    public FormaPagamento[] formasPagamento() {
        return FormaPagamento.values();
    }

    @ModelAttribute("tiposEntrega")
    public TipoEntrega[] tiposEntrega() {
        return TipoEntrega.values();
    }

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String filtro,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataAgendada,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(defaultValue = "dataAgendada") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direcao,
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

        Page<PedidoResponse> paginaPedidos = pedidoService.listarPaginado(
                filtroTratado,
                status,
                dataAgendada,
                pageable
        );

        model.addAttribute("paginaPedidos", paginaPedidos);
        model.addAttribute("pedidos", paginaPedidos.getContent());
        model.addAttribute("pedidosHoje", pedidoService.contarPedidosDeHoje());
        model.addAttribute("dataHoje", LocalDate.now());
        model.addAttribute("filtro", filtroTratado);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("dataAgendada", dataAgendada);
        model.addAttribute("ordenarPor", campoOrdenacao);
        model.addAttribute("direcao", direcaoOrdenacao.name().toLowerCase());
        model.addAttribute("tamanho", tamanhoValido);
        Map<Long, Set<StatusPedido>> transicoesStatus = paginaPedidos.getContent()
                .stream()
                .collect(Collectors.toMap(
                        PedidoResponse::id,
                        pedido -> pedidoService.transicoesPermitidas(
                                pedido.status(),
                                pedido.tipoEntrega()
                        )
                ));
        model.addAttribute("transicoesStatus", transicoesStatus);

        return "pedidos/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        PedidoRequest pedido = PedidoRequest.builder()
                .dataAgendada(LocalDate.now())
                .taxaEntrega(BigDecimal.ZERO)
                .itens(List.of())
                .build();

        model.addAttribute("pedido", pedido);
        prepararFormulario(model, pedido, null, List.of());

        return "pedidos/formulario";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("pedido") PedidoRequest pedido,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "salvar") String acao,
            @RequestParam(name = "itemIds", required = false) List<String> itemIds,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararFormulario(model, pedido, null, itemIds);
            return "pedidos/formulario";
        }

        try {
            PedidoResponse pedidoSalvo = pedidoService.salvar(pedido);

            if ("salvarImprimir".equals(acao)) {
                return "redirect:/pedidos/" + pedidoSalvo.id() + "/comanda";
            }

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Pedido cadastrado com sucesso."
            );
            return "redirect:/pedidos";
        } catch (BusinessException | ResourceNotFoundException exception) {
            bindingResult.reject("pedido.invalido", exception.getMessage());
            prepararFormulario(model, pedido, null, itemIds);
            return "pedidos/formulario";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {
        PedidoResponse pedidoSalvo = pedidoService.buscarPorId(id);
        PedidoRequest pedido = converterParaRequest(pedidoSalvo);

        model.addAttribute("pedido", pedido);
        List<String> itemIds = pedidoSalvo.itens() == null
                ? List.of()
                : pedidoSalvo.itens().stream()
                    .map(item -> item.id().toString())
                    .toList();
        prepararFormulario(model, pedido, id, itemIds);

        return "pedidos/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("pedido") PedidoRequest pedido,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "salvar") String acao,
            @RequestParam(name = "itemIds", required = false) List<String> itemIds,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararFormulario(model, pedido, id, itemIds);
            return "pedidos/formulario";
        }

        try {
            PedidoResponse pedidoAtualizado = pedidoService.atualizar(
                    id,
                    pedido,
                    converterItemIds(itemIds, pedido.itens().size())
            );

            if ("salvarImprimir".equals(acao)) {
                return "redirect:/pedidos/" + pedidoAtualizado.id() + "/comanda";
            }

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Pedido atualizado com sucesso."
            );
            return "redirect:/pedidos";
        } catch (BusinessException | ResourceNotFoundException exception) {
            bindingResult.reject("pedido.invalido", exception.getMessage());
            prepararFormulario(model, pedido, id, itemIds);
            return "pedidos/formulario";
        }
    }

    @PostMapping("/{id}/status")
    public String alterarStatus(
            @PathVariable Long id,
            @RequestParam StatusPedido novoStatus,
            @RequestParam(defaultValue = "") String filtro,
            @RequestParam(required = false) StatusPedido statusFiltro,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataAgendada,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(defaultValue = "dataAgendada") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direcao,
            RedirectAttributes redirectAttributes
    ) {
        try {
            PedidoResponse pedido = pedidoService.alterarStatus(id, novoStatus);
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Pedido " + pedido.id() + " alterado para "
                            + pedido.status().getDescricao() + "."
            );
        } catch (BusinessException | ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        }

        redirectAttributes.addAttribute("filtro", filtro == null ? "" : filtro.trim());
        redirectAttributes.addAttribute("pagina", Math.max(pagina, 0));
        redirectAttributes.addAttribute("tamanho", validarTamanhoPagina(tamanho));
        redirectAttributes.addAttribute("ordenarPor", validarCampoOrdenacao(ordenarPor));
        redirectAttributes.addAttribute(
                "direcao",
                converterDirecao(direcao).name().toLowerCase()
        );

        if (statusFiltro != null) {
            redirectAttributes.addAttribute("status", statusFiltro);
        }

        if (dataAgendada != null) {
            redirectAttributes.addAttribute("dataAgendada", dataAgendada);
        }

        return "redirect:/pedidos";
    }

    @GetMapping("/clientes/buscar")
    @ResponseBody
    public List<ClienteResponse> buscarClientes(
            @RequestParam(defaultValue = "") String termo
    ) {
        return clienteService.buscarPorNomeOuTelefone(termo);
    }

    @PostMapping("/clientes")
    @ResponseBody
    public ResponseEntity<?> cadastrarCliente(
            @Valid @ModelAttribute ClienteRequest cliente,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            Map<String, String> erros = bindingResult.getFieldErrors()
                    .stream()
                    .collect(Collectors.toMap(
                            erro -> erro.getField(),
                            erro -> erro.getDefaultMessage() == null
                                    ? "Valor inválido."
                                    : erro.getDefaultMessage(),
                            (primeiro, segundo) -> primeiro
                    ));
            return ResponseEntity.badRequest().body(erros);
        }

        return ResponseEntity.ok(clienteService.salvar(cliente));
    }

    @GetMapping("/produtos/buscar")
    @ResponseBody
    public List<ProdutoResponse> buscarProdutos(
            @RequestParam(defaultValue = "") String termo
    ) {
        return produtoService.buscarAtivosPorNome(termo);
    }

    @GetMapping("/{id}/comanda")
    public String imprimirComanda(
            @PathVariable Long id,
            Model model
    ) {
        model.addAttribute(
                "pedidos",
                List.of(pedidoService.buscarPorId(id))
        );

        return "pedidos/comandas";
    }

    @GetMapping("/comandas")
    public String imprimirComandas(
            @RequestParam List<Long> ids,
            Model model
    ) {
        model.addAttribute(
                "pedidos",
                pedidoService.buscarPorIds(ids)
        );

        return "pedidos/comandas";
    }

    private void prepararFormulario(
            Model model,
            PedidoRequest pedido,
            Long pedidoId,
            List<String> itemIds
    ) {
        boolean edicao = pedidoId != null;
        model.addAttribute("pedidoId", pedidoId);
        model.addAttribute("titulo", edicao ? "Editar Pedido" : "Novo Pedido");
        model.addAttribute(
                "descricaoFormulario",
                edicao
                        ? "Atualize o cliente, os itens e os dados do pedido"
                        : "Selecione o cliente, organize os itens e confira o total"
        );
        model.addAttribute("urlFormulario", edicao ? "/pedidos/" + pedidoId : "/pedidos");
        model.addAttribute("textoSalvar", edicao ? "Salvar alterações" : "Salvar pedido");
        model.addAttribute("itemIds", itemIds == null ? List.of() : itemIds);
        model.addAttribute("dataMinima", LocalDate.now());

        if (pedido.clienteId() != null) {
            try {
                model.addAttribute(
                        "clienteSelecionado",
                        clienteService.buscarPorId(pedido.clienteId())
                );
            } catch (ResourceNotFoundException ignored) {
                // A validação do serviço exibirá a mensagem ao tentar salvar.
            }
        }

        Map<Long, ProdutoResponse> produtosSelecionados = new HashMap<>();

        if (pedido.itens() != null) {
            pedido.itens().stream()
                    .filter(item -> item != null && item.produtoId() != null)
                    .forEach(item -> {
                        try {
                            ProdutoResponse produto =
                                    produtoService.buscarPorId(item.produtoId());
                            produtosSelecionados.put(produto.id(), produto);
                        } catch (ResourceNotFoundException ignored) {
                            produtosSelecionados.put(
                                    item.produtoId(),
                                    ProdutoResponse.builder()
                                            .id(item.produtoId())
                                            .nome("Produto não encontrado")
                                            .preco(BigDecimal.ZERO)
                                            .unidadeVenda(UnidadeVenda.UNIDADE)
                                            .permiteAcompanhamento(false)
                                            .ativo(false)
                                            .build()
                            );
                        }
                    });
        }

        model.addAttribute("produtosSelecionados", produtosSelecionados);
    }

    private List<Long> converterItemIds(
            List<String> valores,
            int quantidadeItens
    ) {
        List<String> valoresSeguros = valores == null ? List.of() : valores;

        if (valoresSeguros.size() != quantidadeItens) {
            throw new BusinessException("Os itens informados para edição são inválidos.");
        }

        return valoresSeguros.stream()
                .map(valor -> {
                    if (valor == null || valor.isBlank()) {
                        return (Long) null;
                    }

                    try {
                        return Long.valueOf(valor);
                    } catch (NumberFormatException exception) {
                        throw new BusinessException("Identificador de item inválido.");
                    }
                })
                .toList();
    }

    private PedidoRequest converterParaRequest(PedidoResponse pedido) {
        List<ItemPedidoRequest> itens = pedido.itens() == null
                ? List.of()
                : pedido.itens().stream()
                    .map(item -> ItemPedidoRequest.builder()
                            .produtoId(item.produtoId())
                            .quantidade(item.quantidade())
                            .observacao(item.observacao())
                            .build())
                    .toList();

        return PedidoRequest.builder()
                .clienteId(pedido.clienteId())
                .dataAgendada(pedido.dataAgendada())
                .formaPagamento(pedido.formaPagamento())
                .tipoEntrega(pedido.tipoEntrega())
                .taxaEntrega(pedido.taxaEntrega())
                .observacao(pedido.observacao())
                .itens(itens)
                .build();
    }

    private String validarCampoOrdenacao(String ordenarPor) {
        return CAMPOS_ORDENACAO.contains(ordenarPor)
                ? ordenarPor
                : "dataAgendada";
    }

    private Sort.Direction converterDirecao(String direcao) {
        return "desc".equalsIgnoreCase(direcao)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
    }

    private int validarTamanhoPagina(int tamanho) {
        return tamanho == 10 || tamanho == 20 || tamanho == 50
                ? tamanho
                : 10;
    }
}
