package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.configuracao.ConfiguracaoEmpresaResponse;
import br.com.sergio.gestaopedidos.service.ConfiguracaoEmpresaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "br.com.sergio.gestaopedidos.controller.web")
@RequiredArgsConstructor
public class ConfiguracaoGlobalAdvice {

    private final ConfiguracaoEmpresaService configuracaoEmpresaService;

    @ModelAttribute("configuracaoEmpresa")
    public ConfiguracaoEmpresaResponse configuracaoEmpresa() {
        return configuracaoEmpresaService.getConfiguracaoAtual();
    }

    @ModelAttribute("currentRequestPath")
    public String currentRequestPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}
