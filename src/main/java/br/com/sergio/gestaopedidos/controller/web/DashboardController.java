package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String home(Model model) {
        LocalDate dataReferencia = LocalDate.now();

        model.addAttribute(
                "indicadores",
                dashboardService.buscarIndicadores(dataReferencia)
        );
        model.addAttribute("dataReferencia", dataReferencia);
        model.addAttribute("dataReferenciaIso", dataReferencia.toString());

        return "dashboard/dashboard";
    }

}
