package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.service.DashboardService;
import br.com.sergio.gestaopedidos.service.DashboardAnaliticoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardAnaliticoService dashboardAnaliticoService;

    @GetMapping("/")
    public String home(Model model) {
        LocalDate dataReferencia = LocalDate.now();

        var dashboard = dashboardService.buscarDashboard(dataReferencia);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("analitico", dashboardAnaliticoService.buscar(dataReferencia));
        model.addAttribute("indicadores", dashboard.pedidos());
        model.addAttribute("dataReferencia", dataReferencia);
        model.addAttribute("dataReferenciaIso", dataReferencia.toString());
        model.addAttribute(
                "pedidosAtencao",
                dashboard.pedidosAtencao()
        );
        model.addAttribute(
                "resumoStatus",
                dashboard.resumoStatus()
        );
        model.addAttribute(
                "resumoVendasDia",
                new br.com.sergio.gestaopedidos.dto.resumo.ResumoVendasDiaResponse(dataReferencia,
                        dashboard.produtosVendidos(), dashboard.pedidos().faturamentoProdutos(),
                        dashboard.pedidos().taxasEntrega(), dashboard.pedidos().faturamentoDoDia())
        );

        return "dashboard/dashboard";
    }

    @GetMapping("/dashboard/clientes/{clienteId}/pedidos")
    public String historicoCliente(@PathVariable Long clienteId,
                                   @RequestParam(defaultValue = "0") int pagina,
                                   @RequestParam(defaultValue = "5") int tamanho,
                                   Model model) {
        model.addAttribute("historico", dashboardAnaliticoService.buscarHistoricoCliente(clienteId, pagina, tamanho));
        return "dashboard/fragments/historico-cliente :: conteudo";
    }

    @GetMapping("/dashboard/clientes/{clienteId}/pedidos/{pedidoId}/itens")
    public String itensHistoricos(@PathVariable Long clienteId, @PathVariable Long pedidoId, Model model) {
        model.addAttribute("detalhes", dashboardAnaliticoService.buscarItensHistoricos(clienteId, pedidoId));
        return "dashboard/fragments/itens-historicos :: conteudo";
    }

}
