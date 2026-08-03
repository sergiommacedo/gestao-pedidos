package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoLinhaResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.RelatorioPedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioWebController {

    private static final Set<Integer> TAMANHOS_PERMITIDOS = Set.of(10, 20, 50);
    private static final Map<String, String> CAMPOS_ORDENACAO = Map.of(
            "id", "id",
            "dataAgendada", "dataAgendada",
            "cliente", "cliente.nome",
            "status", "status",
            "valorTotal", "valorTotal"
    );

    private final RelatorioPedidoService relatorioPedidoService;

    @GetMapping
    public String index() {
        return "relatorios/index";
    }

    @GetMapping("/pedidos")
    public String pedidos(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataInicial,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate dataFinal,
            @RequestParam(defaultValue = "") String cliente,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(defaultValue = "dataAgendada") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao,
            Model model
    ) {
        LocalDate hoje = LocalDate.now();
        if (dataInicial == null && dataFinal == null) {
            dataInicial = hoje.withDayOfMonth(1);
            dataFinal = hoje;
        }

        String campoSelecionado = CAMPOS_ORDENACAO.containsKey(ordenarPor)
                ? ordenarPor
                : "dataAgendada";
        Sort.Direction direcaoSelecionada = "asc".equalsIgnoreCase(direcao)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        int tamanhoSelecionado = TAMANHOS_PERMITIDOS.contains(tamanho) ? tamanho : 10;
        int paginaSelecionada = Math.max(pagina, 0);
        Sort ordenacao = Sort.by(direcaoSelecionada, CAMPOS_ORDENACAO.get(campoSelecionado));
        if (!"id".equals(campoSelecionado)) {
            ordenacao = ordenacao.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        PageRequest pageable = PageRequest.of(paginaSelecionada, tamanhoSelecionado, ordenacao);

        RelatorioPedidoService.FiltroRelatorioPedidos filtro =
                new RelatorioPedidoService.FiltroRelatorioPedidos(
                        dataInicial,
                        dataFinal,
                        cliente,
                        status,
                        tipoEntrega,
                        formaPagamento
                );

        Page<RelatorioPedidoLinhaResponse> pedidos;
        RelatorioPedidoIndicadoresResponse indicadores;
        try {
            RelatorioPedidoService.ResultadoRelatorioPedidos resultado =
                    relatorioPedidoService.buscar(filtro, pageable);
            pedidos = resultado.pedidos();
            indicadores = resultado.indicadores();
        } catch (BusinessException exception) {
            pedidos = Page.empty(pageable);
            indicadores = RelatorioPedidoIndicadoresResponse.vazio();
            model.addAttribute("erroPeriodo", exception.getMessage());
        }

        model.addAttribute("pedidos", pedidos);
        model.addAttribute("indicadores", indicadores);
        model.addAttribute("dataInicial", dataInicial);
        model.addAttribute("dataFinal", dataFinal);
        model.addAttribute("cliente", cliente == null ? "" : cliente.trim());
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("tipoEntregaSelecionado", tipoEntrega);
        model.addAttribute("formaPagamentoSelecionada", formaPagamento);
        model.addAttribute("statusPedidos", StatusPedido.values());
        model.addAttribute("tiposEntrega", TipoEntrega.values());
        model.addAttribute("formasPagamento", FormaPagamento.values());
        model.addAttribute("tamanho", tamanhoSelecionado);
        model.addAttribute("ordenarPor", campoSelecionado);
        model.addAttribute("direcao", direcaoSelecionada.name().toLowerCase());

        return "relatorios/pedidos";
    }
}
