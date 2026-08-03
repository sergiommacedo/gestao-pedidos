package br.com.sergio.gestaopedidos.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/relatorios")
public class RelatorioWebController {

    @GetMapping
    public String index() {
        return "relatorios/index";
    }
}
