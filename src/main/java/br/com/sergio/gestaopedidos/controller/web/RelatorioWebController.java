package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoLinhaResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioProducaoIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioProducaoLinhaResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.ConfiguracaoEmpresaService;
import br.com.sergio.gestaopedidos.service.RelatorioPedidoExcelService;
import br.com.sergio.gestaopedidos.service.RelatorioPedidoService;
import br.com.sergio.gestaopedidos.service.RelatorioProducaoExcelService;
import br.com.sergio.gestaopedidos.service.RelatorioProducaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final Map<String, String> CAMPOS_ORDENACAO_PRODUCAO = Map.of(
            "produto", "produto.nome",
            "quantidade", "SUM(item.quantidade)",
            "pedidos", "COUNT(DISTINCT pedido.id)",
            "faturamento", "SUM(item.subtotal)",
            "media", "SUM(item.subtotal) / COUNT(DISTINCT pedido.id)",
            "participacao", "SUM(item.subtotal)"
    );

    private final RelatorioPedidoService relatorioPedidoService;
    private final RelatorioPedidoExcelService relatorioPedidoExcelService;
    private final ConfiguracaoEmpresaService configuracaoEmpresaService;
    private final RelatorioProducaoService relatorioProducaoService;
    private final RelatorioProducaoExcelService relatorioProducaoExcelService;

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

    @GetMapping("/pedidos/imprimir")
    public String imprimirPedidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(defaultValue = "") String cliente,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @RequestParam(defaultValue = "dataAgendada") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        RelatorioPedidoService.FiltroRelatorioPedidos filtro = criarFiltro(
                dataInicial, dataFinal, cliente, status, tipoEntrega, formaPagamento
        );
        try {
            RelatorioPedidoService.ResultadoCompletoRelatorioPedidos resultado =
                    relatorioPedidoService.buscarParaSaida(
                            filtro,
                            criarOrdenacao(ordenarPor, direcao)
                    );
            model.addAttribute("pedidos", resultado.pedidos());
            model.addAttribute("indicadores", resultado.indicadores());
            model.addAttribute("dataInicial", dataInicial);
            model.addAttribute("dataFinal", dataFinal);
            model.addAttribute("cliente", cliente == null ? "" : cliente.trim());
            model.addAttribute("statusSelecionado", status);
            model.addAttribute("tipoEntregaSelecionado", tipoEntrega);
            model.addAttribute("formaPagamentoSelecionada", formaPagamento);
            model.addAttribute("ordenarPor", normalizarCampoOrdenacao(ordenarPor));
            model.addAttribute("direcao", normalizarDirecao(direcao).name().toLowerCase());
            model.addAttribute("emitidoEm", LocalDateTime.now());
            return "relatorios/pedidos-impressao";
        } catch (BusinessException exception) {
            return redirecionarComErro(filtro, ordenarPor, direcao, exception, redirectAttributes);
        }
    }

    @GetMapping("/pedidos/excel")
    public ResponseEntity<byte[]> exportarPedidosExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(defaultValue = "") String cliente,
            @RequestParam(required = false) StatusPedido status,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @RequestParam(defaultValue = "dataAgendada") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao
    ) {
        RelatorioPedidoService.FiltroRelatorioPedidos filtro = criarFiltro(
                dataInicial, dataFinal, cliente, status, tipoEntrega, formaPagamento
        );
        RelatorioPedidoService.ResultadoCompletoRelatorioPedidos resultado =
                relatorioPedidoService.buscarParaSaida(filtro, criarOrdenacao(ordenarPor, direcao));
        byte[] arquivo = relatorioPedidoExcelService.gerar(
                configuracaoEmpresaService.getConfiguracaoAtual().nomeEmpresa(),
                dataInicial,
                dataFinal,
                LocalDateTime.now(),
                resultado.pedidos(),
                resultado.indicadores()
        );
        String nomeArquivo = "relatorio-pedidos-"
                + dataInicial.format(DateTimeFormatter.ISO_DATE)
                + "-a-"
                + dataFinal.format(DateTimeFormatter.ISO_DATE)
                + ".xlsx";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + nomeArquivo + "\""
                )
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(arquivo);
    }

    @GetMapping("/producao")
    public String producao(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(defaultValue = "") String produto,
            @RequestParam(required = false) UnidadeVenda unidadeVenda,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(defaultValue = "faturamento") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao,
            Model model
    ) {
        LocalDate hoje = LocalDate.now();
        if (dataInicial == null && dataFinal == null) { dataInicial = hoje.withDayOfMonth(1); dataFinal = hoje; }
        int tamanhoSeguro = TAMANHOS_PERMITIDOS.contains(tamanho) ? tamanho : 10;
        String campo = normalizarCampoProducao(ordenarPor);
        Sort.Direction sentido = normalizarDirecao(direcao);
        PageRequest pageable = PageRequest.of(Math.max(0, pagina), tamanhoSeguro, criarOrdenacaoProducao(campo, sentido));
        RelatorioProducaoService.FiltroRelatorioProducao filtro = criarFiltroProducao(
                dataInicial, dataFinal, produto, unidadeVenda, tipoEntrega, formaPagamento
        );
        Page<RelatorioProducaoLinhaResponse> linhas;
        RelatorioProducaoIndicadoresResponse indicadores;
        try {
            var resultado = relatorioProducaoService.buscar(filtro, pageable);
            linhas = resultado.linhas(); indicadores = resultado.indicadores();
        } catch (BusinessException exception) {
            linhas = Page.empty(pageable); indicadores = RelatorioProducaoIndicadoresResponse.vazio();
            model.addAttribute("erroPeriodo", exception.getMessage());
        }
        adicionarModeloProducao(model, filtro, campo, sentido, tamanhoSeguro);
        model.addAttribute("linhas", linhas); model.addAttribute("indicadores", indicadores);
        return "relatorios/producao";
    }

    @GetMapping("/producao/imprimir")
    public String imprimirProducao(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(defaultValue = "") String produto,
            @RequestParam(required = false) UnidadeVenda unidadeVenda,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @RequestParam(defaultValue = "faturamento") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao,
            Model model, RedirectAttributes redirectAttributes
    ) {
        var filtro = criarFiltroProducao(dataInicial, dataFinal, produto, unidadeVenda, tipoEntrega, formaPagamento);
        try {
            var resultado = relatorioProducaoService.buscarParaSaida(
                    filtro, criarOrdenacaoProducao(normalizarCampoProducao(ordenarPor), normalizarDirecao(direcao))
            );
            adicionarModeloProducao(model, filtro, normalizarCampoProducao(ordenarPor), normalizarDirecao(direcao), 10);
            model.addAttribute("linhas", resultado.linhas()); model.addAttribute("indicadores", resultado.indicadores());
            model.addAttribute("emitidoEm", LocalDateTime.now());
            return "relatorios/producao-impressao";
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute("erroExportacao", exception.getMessage());
            adicionarRedirectProducao(redirectAttributes, filtro, ordenarPor, direcao);
            return "redirect:/relatorios/producao";
        }
    }

    @GetMapping("/producao/excel")
    public ResponseEntity<byte[]> exportarProducaoExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(defaultValue = "") String produto,
            @RequestParam(required = false) UnidadeVenda unidadeVenda,
            @RequestParam(required = false) TipoEntrega tipoEntrega,
            @RequestParam(required = false) FormaPagamento formaPagamento,
            @RequestParam(defaultValue = "faturamento") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao
    ) {
        var filtro = criarFiltroProducao(dataInicial, dataFinal, produto, unidadeVenda, tipoEntrega, formaPagamento);
        var resultado = relatorioProducaoService.buscarParaSaida(
                filtro, criarOrdenacaoProducao(normalizarCampoProducao(ordenarPor), normalizarDirecao(direcao))
        );
        byte[] arquivo = relatorioProducaoExcelService.gerar(
                configuracaoEmpresaService.getConfiguracaoAtual().nomeEmpresa(), dataInicial, dataFinal,
                LocalDateTime.now(), resultado.linhas(), resultado.indicadores()
        );
        String nome = "relatorio-producao-" + dataInicial + "-a-" + dataFinal + ".xlsx";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(arquivo);
    }

    private RelatorioProducaoService.FiltroRelatorioProducao criarFiltroProducao(
            LocalDate inicio, LocalDate fim, String produto, UnidadeVenda unidade,
            TipoEntrega entrega, FormaPagamento pagamento
    ) { return new RelatorioProducaoService.FiltroRelatorioProducao(inicio, fim, produto, unidade, entrega, pagamento); }

    private String normalizarCampoProducao(String campo) {
        return CAMPOS_ORDENACAO_PRODUCAO.containsKey(campo) ? campo : "faturamento";
    }

    private Sort criarOrdenacaoProducao(String campo, Sort.Direction direcao) {
        Sort sort = JpaSort.unsafe(direcao, CAMPOS_ORDENACAO_PRODUCAO.get(campo));
        return "produto".equals(campo) ? sort : sort.and(Sort.by(Sort.Direction.ASC, "produto.nome"));
    }

    private void adicionarModeloProducao(Model model, RelatorioProducaoService.FiltroRelatorioProducao f,
                                         String campo, Sort.Direction direcao, int tamanho) {
        model.addAttribute("dataInicial", f.dataInicial()); model.addAttribute("dataFinal", f.dataFinal());
        model.addAttribute("produto", f.produto() == null ? "" : f.produto().trim());
        model.addAttribute("unidadeSelecionada", f.unidadeVenda()); model.addAttribute("tipoEntregaSelecionado", f.tipoEntrega());
        model.addAttribute("formaPagamentoSelecionada", f.formaPagamento()); model.addAttribute("unidadesVenda", UnidadeVenda.values());
        model.addAttribute("tiposEntrega", TipoEntrega.values()); model.addAttribute("formasPagamento", FormaPagamento.values());
        model.addAttribute("ordenarPor", campo); model.addAttribute("direcao", direcao.name().toLowerCase()); model.addAttribute("tamanho", tamanho);
    }

    private void adicionarRedirectProducao(RedirectAttributes r, RelatorioProducaoService.FiltroRelatorioProducao f,
                                           String campo, String direcao) {
        r.addAttribute("dataInicial", f.dataInicial()); r.addAttribute("dataFinal", f.dataFinal());
        r.addAttribute("produto", f.produto()); if (f.unidadeVenda() != null) r.addAttribute("unidadeVenda", f.unidadeVenda());
        if (f.tipoEntrega() != null) r.addAttribute("tipoEntrega", f.tipoEntrega());
        if (f.formaPagamento() != null) r.addAttribute("formaPagamento", f.formaPagamento());
        r.addAttribute("ordenarPor", normalizarCampoProducao(campo)); r.addAttribute("direcao", normalizarDirecao(direcao).name().toLowerCase());
    }

    private RelatorioPedidoService.FiltroRelatorioPedidos criarFiltro(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String cliente,
            StatusPedido status,
            TipoEntrega tipoEntrega,
            FormaPagamento formaPagamento
    ) {
        return new RelatorioPedidoService.FiltroRelatorioPedidos(
                dataInicial,
                dataFinal,
                cliente,
                status,
                tipoEntrega,
                formaPagamento
        );
    }

    private Sort criarOrdenacao(String ordenarPor, String direcao) {
        String campo = normalizarCampoOrdenacao(ordenarPor);
        Sort ordenacao = Sort.by(normalizarDirecao(direcao), CAMPOS_ORDENACAO.get(campo));
        return "id".equals(campo)
                ? ordenacao
                : ordenacao.and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private String normalizarCampoOrdenacao(String ordenarPor) {
        return CAMPOS_ORDENACAO.containsKey(ordenarPor) ? ordenarPor : "dataAgendada";
    }

    private Sort.Direction normalizarDirecao(String direcao) {
        return "asc".equalsIgnoreCase(direcao) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private String redirecionarComErro(
            RelatorioPedidoService.FiltroRelatorioPedidos filtro,
            String ordenarPor,
            String direcao,
            BusinessException exception,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute("erroExportacao", exception.getMessage());
        redirectAttributes.addAttribute("dataInicial", filtro.dataInicial());
        redirectAttributes.addAttribute("dataFinal", filtro.dataFinal());
        redirectAttributes.addAttribute("cliente", filtro.cliente());
        if (filtro.status() != null) redirectAttributes.addAttribute("status", filtro.status());
        if (filtro.tipoEntrega() != null) redirectAttributes.addAttribute("tipoEntrega", filtro.tipoEntrega());
        if (filtro.formaPagamento() != null) redirectAttributes.addAttribute("formaPagamento", filtro.formaPagamento());
        redirectAttributes.addAttribute("ordenarPor", normalizarCampoOrdenacao(ordenarPor));
        redirectAttributes.addAttribute("direcao", normalizarDirecao(direcao).name().toLowerCase());
        return "redirect:/relatorios/pedidos";
    }
}
