package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.usuario.AlterarSenhaInicialRequest;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AlterarSenhaInicialWebController {

    private final UsuarioService usuarioService;

    @GetMapping("/alterar-senha-inicial")
    public String formulario(Model model) {
        if (!model.containsAttribute("alteracaoSenha")) {
            model.addAttribute(
                    "alteracaoSenha",
                    new AlterarSenhaInicialRequest(null, null, null)
            );
        }

        return "alterar-senha-inicial";
    }

    @PostMapping("/alterar-senha-inicial")
    public String alterar(
            @Valid @ModelAttribute("alteracaoSenha") AlterarSenhaInicialRequest request,
            BindingResult bindingResult,
            Authentication authentication,
            HttpServletRequest httpServletRequest
    ) {
        if (!bindingResult.hasErrors()) {
            try {
                usuarioService.alterarSenhaInicial(authentication.getName(), request);
            } catch (BusinessException exception) {
                bindingResult.reject("alteracaoSenha.invalida", exception.getMessage());
            }
        }

        if (bindingResult.hasErrors()) {
            return "alterar-senha-inicial";
        }

        HttpSession sessao = httpServletRequest.getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        SecurityContextHolder.clearContext();

        return "redirect:/login?senhaAlterada";
    }
}
