package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.cliente.ClienteRequest;
import br.com.sergio.gestaopedidos.dto.cliente.ClienteResponse;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.dto.pedido.ItemPedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PlanejamentoEntregasResponse;
import br.com.sergio.gestaopedidos.dto.produto.ProdutoResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import br.com.sergio.gestaopedidos.service.ClienteService;
import br.com.sergio.gestaopedidos.service.DashboardService;
import br.com.sergio.gestaopedidos.service.PedidoService;
import br.com.sergio.gestaopedidos.service.EstoqueService;
import br.com.sergio.gestaopedidos.service.ProdutoService;
import br.com.sergio.gestaopedidos.service.ConfiguracaoEmpresaService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
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
    private final EstoqueService estoqueService;
    private final DashboardService dashboardService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final ConfiguracaoEmpresaService configuracaoEmpresaService;

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
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @RequestParam(defaultValue = "") String situacaoEstoque,
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
                dataAgendada == null ? LocalDate.now() : dataAgendada,
                tipoEntrega,
                formaPagamento,
                situacaoEstoque,
                pageable
        );

        model.addAttribute("paginaPedidos", paginaPedidos);
        model.addAttribute("pedidos", paginaPedidos.getContent());
        LocalDate dataDashboard = dataAgendada == null ? LocalDate.now() : dataAgendada;
        model.addAttribute(
                "dashboardOperacional",
                pedidoService.buscarDashboardOperacional(dataDashboard)
        );
        model.addAttribute(
                "resumoVendasDia",
                dashboardService.buscarResumoVendasDia(dataDashboard)
        );
        model.addAttribute("dataDashboardIso", dataDashboard.toString());
        model.addAttribute("filtro", filtroTratado);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("dataAgendada", dataAgendada);
        model.addAttribute("tipoEntregaSelecionado", tipoEntrega);
        model.addAttribute("formaPagamentoSelecionada", formaPagamento);
        model.addAttribute("situacaoEstoqueSelecionada", situacaoEstoque);
        model.addAttribute("situacoesEstoque", pedidoService.situacoesEstoque(paginaPedidos.getContent()));
        model.addAttribute(
                "dataAgendadaIso",
                dataAgendada == null ? LocalDate.now().toString() : dataAgendada.toString()
        );
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
        adicionarPermissoesAcoes(model);

        return "pedidos/listar";
    }

    @GetMapping("/kanban")
    public String kanban(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataAgendada,
            @RequestParam(defaultValue = "false") boolean mostrarCancelados,
            Model model
    ) {
        LocalDate dataSelecionada = dataAgendada == null
                ? LocalDate.now()
                : dataAgendada;
        List<PedidoResponse> pedidos = pedidoService.listarPorData(dataSelecionada);

        model.addAttribute("dataAgendada", dataSelecionada);
        model.addAttribute("dataAgendadaIso", dataSelecionada.toString());
        model.addAttribute("mostrarCancelados", mostrarCancelados);
        model.addAttribute("statusCancelado", StatusPedido.CANCELADO);
        model.addAttribute(
                "pedidosCancelados",
                mostrarCancelados
                        ? filtrarPorStatus(pedidos, StatusPedido.CANCELADO)
                        : List.of()
        );
        Map<StatusPedido, List<PedidoResponse>> pedidosPorStatus = new EnumMap<>(StatusPedido.class);
        pedidosPorStatus.put(StatusPedido.PENDENTE, filtrarPorStatus(pedidos, StatusPedido.PENDENTE));
        pedidosPorStatus.put(StatusPedido.EM_PREPARACAO, filtrarPorStatus(pedidos, StatusPedido.EM_PREPARACAO));
        pedidosPorStatus.put(StatusPedido.PRONTO, filtrarPorStatus(pedidos, StatusPedido.PRONTO));
        pedidosPorStatus.put(
                StatusPedido.SAIU_PARA_ENTREGA,
                filtrarPorStatus(pedidos, StatusPedido.SAIU_PARA_ENTREGA)
        );
        pedidosPorStatus.put(StatusPedido.ENTREGUE, filtrarPorStatus(pedidos, StatusPedido.ENTREGUE));
        model.addAttribute("pedidosPorStatus", pedidosPorStatus);
        model.addAttribute(
                "statusKanban",
                List.of(
                        StatusPedido.PENDENTE,
                        StatusPedido.EM_PREPARACAO,
                        StatusPedido.PRONTO,
                        StatusPedido.SAIU_PARA_ENTREGA,
                        StatusPedido.ENTREGUE
                )
        );

        Map<Long, Set<StatusPedido>> transicoesStatus = pedidos.stream()
                .collect(Collectors.toMap(
                        PedidoResponse::id,
                        pedido -> pedidoService.transicoesPermitidas(
                                pedido.status(),
                                pedido.tipoEntrega()
                        )
                ));
        Map<Long, StatusPedido> proximosStatus = pedidos.stream()
                .collect(Collectors.toMap(
                        PedidoResponse::id,
                        pedido -> transicoesStatus.get(pedido.id()).stream()
                                .filter(status -> status != StatusPedido.CANCELADO)
                                .findFirst()
                                .orElse(pedido.status())
                ));
        model.addAttribute("transicoesStatus", transicoesStatus);
        model.addAttribute("proximosStatus", proximosStatus);
        model.addAttribute("situacoesEstoque", pedidoService.situacoesEstoque(pedidos));
        adicionarPermissoesAcoes(model);

        return "pedidos/kanban";
    }

    @GetMapping("/novo")
    public String novo(
            @RequestParam(defaultValue = "lista") String origem,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAgendada,
            @RequestParam(defaultValue = "false") boolean mostrarCancelados,
            Model model
    ) {
        PedidoRequest pedido = PedidoRequest.builder()
                .dataAgendada(dataAgendada == null ? LocalDate.now() : dataAgendada)
                .taxaEntrega(BigDecimal.ZERO)
                .itens(List.of())
                .build();

        model.addAttribute("pedido", pedido);
        prepararFormulario(model, pedido, null, List.of());
        adicionarOrigemFormulario(model, origem, dataAgendada, mostrarCancelados);

        return "pedidos/formulario";
    }

    @GetMapping("/planejamento")
    public String planejamento(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAgendada,
            Model model
    ) {
        LocalDate dataSelecionada = dataAgendada == null ? LocalDate.now() : dataAgendada;
        PlanejamentoEntregasResponse planejamento = pedidoService.planejarEntregas(dataSelecionada);
        model.addAttribute("dataAgendada", dataSelecionada);
        model.addAttribute("entregasPlanejaveis", planejamento.elegiveis());
        model.addAttribute("entregasEmRota", planejamento.emRota());
        model.addAttribute("quantidadeEntregasElegiveis", planejamento.quantidadeElegiveis());
        model.addAttribute("quantidadeEnderecosNavegaveis", planejamento.quantidadeEnderecosNavegaveis());
        model.addAttribute("quantidadeEnderecosIncompletos", planejamento.quantidadeEnderecosIncompletos());
        model.addAttribute("quantidadeEntregasEmRota", planejamento.quantidadeEmRota());
        model.addAttribute("enderecoSaida", configuracaoEmpresaService.getConfiguracaoAtual().enderecoSaidaEntregas());
        return "pedidos/planejamento";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("pedido") PedidoRequest pedido,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "salvar") String acao,
            @RequestParam(name = "itemIds", required = false) List<String> itemIds,
            @RequestParam(defaultValue = "lista") String origem,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataRetornoKanban,
            @RequestParam(defaultValue = "false") boolean mostrarCancelados,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            prepararFormulario(model, pedido, null, itemIds);
            adicionarOrigemFormulario(model, origem, dataRetornoKanban, mostrarCancelados);
            return "pedidos/formulario";
        }

        try {
            PedidoResponse pedidoSalvo = pedidoService.salvar(pedido);

            if ("salvarImprimir".equals(acao)) {
                redirectAttributes.addAttribute("origem", normalizarOrigem(origem));
                if (dataRetornoKanban != null) redirectAttributes.addAttribute("dataAgendada", dataRetornoKanban);
                if (mostrarCancelados) redirectAttributes.addAttribute("mostrarCancelados", true);
                return "redirect:/pedidos/" + pedidoSalvo.id() + "/comanda";
            }

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Pedido cadastrado com sucesso."
            );
            return redirecionarParaOrigem(origem, dataRetornoKanban, mostrarCancelados, redirectAttributes);
        } catch (BusinessException | ResourceNotFoundException exception) {
            bindingResult.reject("pedido.invalido", exception.getMessage());
            prepararFormulario(model, pedido, null, itemIds);
            adicionarOrigemFormulario(model, origem, dataRetornoKanban, mostrarCancelados);
            return "pedidos/formulario";
        }
    }

    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "lista") String origem,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataAgendada,
            @RequestParam(defaultValue = "false") boolean mostrarCancelados,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        PedidoResponse pedidoSalvo;
        try {
            pedidoSalvo = pedidoService.buscarParaEdicao(id);
        } catch (BusinessException | ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return redirecionarParaOrigem(
                    origem,
                    dataAgendada,
                    mostrarCancelados,
                    redirectAttributes
            );
        }
        PedidoRequest pedido = converterParaRequest(pedidoSalvo);

        model.addAttribute("pedido", pedido);
        List<String> itemIds = pedidoSalvo.itens() == null
                ? List.of()
                : pedidoSalvo.itens().stream()
                    .map(item -> item.id().toString())
                    .toList();
        prepararFormulario(model, pedido, id, itemIds);
        adicionarOrigemFormulario(model, origem, dataAgendada, mostrarCancelados);

        return "pedidos/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("pedido") PedidoRequest pedido,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "salvar") String acao,
            @RequestParam(name = "itemIds", required = false) List<String> itemIds,
            @RequestParam(defaultValue = "lista") String origem,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataRetornoKanban,
            @RequestParam(defaultValue = "false") boolean mostrarCancelados,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            pedidoService.buscarParaEdicao(id);
        } catch (BusinessException | ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return redirecionarParaOrigem(
                    origem,
                    dataRetornoKanban,
                    mostrarCancelados,
                    redirectAttributes
            );
        }

        if (bindingResult.hasErrors()) {
            prepararFormulario(model, pedido, id, itemIds);
            adicionarOrigemFormulario(
                    model,
                    origem,
                    dataRetornoKanban,
                    mostrarCancelados
            );
            return "pedidos/formulario";
        }

        try {
            PedidoResponse pedidoAtualizado = pedidoService.atualizar(
                    id,
                    pedido,
                    converterItemIds(itemIds, pedido.itens().size())
            );

            if ("salvarImprimir".equals(acao)) {
                redirectAttributes.addAttribute("origem", origem);
                if (dataRetornoKanban != null) {
                    redirectAttributes.addAttribute(
                            "dataAgendada",
                            dataRetornoKanban.toString()
                    );
                }
                if (mostrarCancelados) {
                    redirectAttributes.addAttribute("mostrarCancelados", true);
                }
                return "redirect:/pedidos/" + pedidoAtualizado.id() + "/comanda";
            }

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Pedido atualizado com sucesso."
            );
            return redirecionarParaOrigem(
                    origem,
                    dataRetornoKanban,
                    mostrarCancelados,
                    redirectAttributes
            );
        } catch (BusinessException | ResourceNotFoundException exception) {
            bindingResult.reject("pedido.invalido", exception.getMessage());
            prepararFormulario(model, pedido, id, itemIds);
            adicionarOrigemFormulario(
                    model,
                    origem,
                    dataRetornoKanban,
                    mostrarCancelados
            );
            return "pedidos/formulario";
        }
    }

    @GetMapping("/{id}/detalhes")
    public String detalhes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "lista") String origem,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataAgendada,
            @RequestParam(defaultValue = "false") boolean mostrarCancelados,
            Model model
    ) {
        PedidoResponse pedido = pedidoService.buscarPorId(id);
        model.addAttribute("pedido", pedido);
        model.addAttribute("movimentacoesEstoque", estoqueService.movimentacoesPedido(id));
        model.addAttribute("situacaoEstoque", pedidoService.situacaoEstoque(pedido));
        String origemNormalizada = normalizarOrigem(origem);
        boolean origemRelatorio = "relatorio".equals(origemNormalizada);
        model.addAttribute(
                "pedidoEditavel",
                !origemRelatorio && pedidoService.isEditavel(pedido.status())
        );
        model.addAttribute(
                "pedidoImprimivel",
                !origemRelatorio && pedidoService.isImprimivel(pedido.status())
        );
        model.addAttribute("origem", origemNormalizada);
        model.addAttribute("dataRetornoKanbanIso", dataAgendada == null
                ? null
                : dataAgendada.toString());
        model.addAttribute("mostrarCancelados", mostrarCancelados);
        return "pedidos/fragments/detalhes :: conteudo";
    }

    @PostMapping("/{id}/status")
    public String alterarStatus(
            @PathVariable Long id,
            @RequestParam StatusPedido novoStatus,
            @RequestParam(required = false) String motivoCancelamento,
            @RequestParam(defaultValue = "") String filtro,
            @RequestParam(required = false) StatusPedido statusFiltro,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @RequestParam(defaultValue = "") String situacaoEstoque,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataAgendada,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(defaultValue = "dataAgendada") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direcao,
            @RequestParam(defaultValue = "lista") String visualizacao,
            @RequestParam(defaultValue = "false") boolean mostrarCancelados,
            RedirectAttributes redirectAttributes
    ) {
        try {
            PedidoResponse pedido = pedidoService.alterarStatus(
                    id,
                    novoStatus,
                    motivoCancelamento
            );
            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Pedido " + pedido.id() + " alterado para "
                            + pedido.status().getDescricao() + "."
            );
        } catch (BusinessException | ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
        }

        if ("kanban".equals(visualizacao)) {
            if (dataAgendada != null) {
                redirectAttributes.addAttribute("dataAgendada", dataAgendada.toString());
            }
            if (mostrarCancelados) {
                redirectAttributes.addAttribute("mostrarCancelados", true);
            }
            return "redirect:/pedidos/kanban";
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
            redirectAttributes.addAttribute("dataAgendada", dataAgendada.toString());
        }
        if (tipoEntrega != null) redirectAttributes.addAttribute("tipoEntrega", tipoEntrega);
        if (formaPagamento != null) redirectAttributes.addAttribute("formaPagamento", formaPagamento);
        if (situacaoEstoque != null && !situacaoEstoque.isBlank()) {
            redirectAttributes.addAttribute("situacaoEstoque", situacaoEstoque);
        }

        return "redirect:/pedidos";
    }

    private List<PedidoResponse> filtrarPorStatus(
            List<PedidoResponse> pedidos,
            StatusPedido status
    ) {
        return pedidos.stream()
                .filter(pedido -> pedido.status() == status)
                .toList();
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
        return produtoService.buscarAtivosEVendaveisPorNome(termo);
    }

    @PostMapping("/preview-estoque")
    @ResponseBody
    public ResponseEntity<?> preverEstoque(@RequestBody PedidoRequest pedido) {
        try {
            return ResponseEntity.ok(pedidoService.preverEstoque(pedido));
        } catch (BusinessException | ResourceNotFoundException exception) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("mensagem", exception.getMessage()));
        }
    }

    @GetMapping("/{id}/comanda")
    public String imprimirComanda(
            @PathVariable Long id,
            @RequestParam(defaultValue = "lista") String origem,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataAgendada,
            @RequestParam(defaultValue = "false") boolean mostrarCancelados,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            prepararComandas(model, List.of(pedidoService.buscarParaImpressao(id)));
        } catch (BusinessException | ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return redirecionarParaOrigem(
                    origem,
                    dataAgendada,
                    mostrarCancelados,
                    redirectAttributes
            );
        }

        return "pedidos/comandas";
    }

    @GetMapping("/comandas")
    public String imprimirComandas(
            @RequestParam List<Long> ids,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            prepararComandas(model, pedidoService.buscarPorIdsParaImpressao(ids));
        } catch (BusinessException | ResourceNotFoundException exception) {
            redirectAttributes.addFlashAttribute("mensagemErro", exception.getMessage());
            return "redirect:/pedidos";
        }

        return "pedidos/comandas";
    }

    private void prepararComandas(
            Model model,
            List<PedidoResponse> pedidos
    ) {
        model.addAttribute("pedidos", pedidos);

        LocalDate dataRetornoKanban = pedidos.isEmpty()
                ? null
                : pedidos.getFirst().dataAgendada();
        boolean mesmaData = dataRetornoKanban != null
                && pedidos.stream()
                    .allMatch(pedido -> dataRetornoKanban.equals(pedido.dataAgendada()));

        model.addAttribute(
                "dataRetornoKanban",
                mesmaData ? dataRetornoKanban : null
        );
        model.addAttribute(
                "dataRetornoKanbanIso",
                mesmaData ? dataRetornoKanban.toString() : null
        );
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
                                            .tipoProduto(br.com.sergio.gestaopedidos.enums.TipoProduto.PREPARACAO_PRODUZIDA)
                                            .vendavel(false)
                                            .build()
                            );
                        }
                    });
        }

        model.addAttribute("produtosSelecionados", produtosSelecionados);
    }

    private void adicionarPermissoesAcoes(Model model) {
        Set<StatusPedido> permitidos = pedidoService.statusEditaveis();
        model.addAttribute("statusEditaveis", permitidos);
        model.addAttribute("statusImprimiveis", permitidos);
    }

    private void adicionarOrigemFormulario(
            Model model,
            String origem,
            LocalDate dataAgendada,
            boolean mostrarCancelados
    ) {
        model.addAttribute("origem", normalizarOrigem(origem));
        model.addAttribute("dataRetornoKanbanIso", dataAgendada == null
                ? ""
                : dataAgendada.toString());
        model.addAttribute("mostrarCancelados", mostrarCancelados);
    }

    private String redirecionarParaOrigem(
            String origem,
            LocalDate dataAgendada,
            boolean mostrarCancelados,
            RedirectAttributes redirectAttributes
    ) {
        if ("kanban".equals(normalizarOrigem(origem))) {
            if (dataAgendada != null) {
                redirectAttributes.addAttribute("dataAgendada", dataAgendada.toString());
            }
            if (mostrarCancelados) {
                redirectAttributes.addAttribute("mostrarCancelados", true);
            }
            return "redirect:/pedidos/kanban";
        }
        return "redirect:/pedidos";
    }

    private String normalizarOrigem(String origem) {
        if ("kanban".equals(origem) || "relatorio".equals(origem)) {
            return origem;
        }
        return "lista";
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
                .horarioInicio(pedido.horarioInicio())
                .horarioFim(pedido.horarioFim())
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
