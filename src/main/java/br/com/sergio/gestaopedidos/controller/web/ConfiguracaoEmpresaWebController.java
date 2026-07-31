package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.configuracao.ConfiguracaoEmpresaRequest;
import br.com.sergio.gestaopedidos.dto.configuracao.ConfiguracaoEmpresaResponse;
import br.com.sergio.gestaopedidos.enums.TemaSistema;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.ConfiguracaoEmpresaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/configuracoes")
@RequiredArgsConstructor
public class ConfiguracaoEmpresaWebController {

    private final ConfiguracaoEmpresaService configuracaoEmpresaService;

    @ModelAttribute("temas")
    public TemaSistema[] temas() {
        return TemaSistema.values();
    }

    @GetMapping
    public String formulario(Model model) {
        ConfiguracaoEmpresaResponse atual = configuracaoEmpresaService.buscarConfiguracao();
        model.addAttribute("configuracao", new ConfiguracaoEmpresaRequest(
                atual.nomeEmpresa(),
                atual.nomeCurto(),
                atual.tema(),
                atual.textoBoasVindas()
        ));
        prepararPreview(model, atual);
        model.addAttribute("removerLogoSelecionada", false);
        return "configuracoes/formulario";
    }

    @PostMapping
    public String atualizar(
            @Valid @ModelAttribute("configuracao") ConfiguracaoEmpresaRequest configuracao,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(defaultValue = "false") boolean removerLogo,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        ConfiguracaoEmpresaResponse atual = configuracaoEmpresaService.buscarConfiguracao();

        if (bindingResult.hasErrors()) {
            prepararPreview(model, atual);
            model.addAttribute("removerLogoSelecionada", removerLogo);
            return "configuracoes/formulario";
        }

        try {
            configuracaoEmpresaService.salvarOuAtualizar(configuracao, logo, removerLogo);
        } catch (BusinessException exception) {
            bindingResult.reject("configuracao.logo.invalida", exception.getMessage());
            prepararPreview(model, atual);
            model.addAttribute("removerLogoSelecionada", removerLogo);
            return "configuracoes/formulario";
        }

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Configurações atualizadas com sucesso."
        );
        return "redirect:/configuracoes";
    }

    private void prepararPreview(Model model, ConfiguracaoEmpresaResponse atual) {
        model.addAttribute("logoAtualUrl", atual.logoUrl());
        model.addAttribute("logoPersonalizada", atual.logoArquivo() != null);
        model.addAttribute("temaAtualCss", atual.temaCss());
        model.addAttribute("logoPadraoUrl", "/img/logo-feijoada-vovo-dan.png");
    }
}
