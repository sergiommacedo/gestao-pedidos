package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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

    @ModelAttribute("statusPedidos")
    public StatusPedido[] statusPedidos() {
        return StatusPedido.values();
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
        model.addAttribute("filtro", filtroTratado);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("dataAgendada", dataAgendada);
        model.addAttribute("ordenarPor", campoOrdenacao);
        model.addAttribute("direcao", direcaoOrdenacao.name().toLowerCase());
        model.addAttribute("tamanho", tamanhoValido);

        return "pedidos/listar";
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
