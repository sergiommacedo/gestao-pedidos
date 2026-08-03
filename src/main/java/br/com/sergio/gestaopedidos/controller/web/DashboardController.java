package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.service.DashboardService;
import br.com.sergio.gestaopedidos.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final PedidoService pedidoService;

    @GetMapping("/")
    public String home(Model model) {
        LocalDate dataReferencia = LocalDate.now();

        model.addAttribute(
                "indicadores",
                dashboardService.buscarIndicadores(dataReferencia)
        );
        model.addAttribute("dataReferencia", dataReferencia);
        model.addAttribute("dataReferenciaIso", dataReferencia.toString());
        model.addAttribute(
                "pedidosAtencao",
                dashboardService.buscarPedidosQuePrecisamAtencao(dataReferencia)
        );
        model.addAttribute("statusEditaveis", pedidoService.statusEditaveis());
        model.addAttribute("statusImprimiveis", pedidoService.statusEditaveis());
        model.addAttribute(
                "resumoStatus",
                dashboardService.buscarResumoStatus(dataReferencia)
        );
        model.addAttribute(
                "resumoVendasDia",
                dashboardService.buscarResumoVendasDia(dataReferencia)
        );

        return "dashboard/dashboard";
    }

}
