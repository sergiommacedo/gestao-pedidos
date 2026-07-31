package br.com.sergio.gestaopedidos.security;

import br.com.sergio.gestaopedidos.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TrocaSenhaObrigatoriaInterceptor implements HandlerInterceptor {

    private final UsuarioService usuarioService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return true;
        }

        if (usuarioService.deveTrocarSenhaNoPrimeiroAcesso(authentication.getName())) {
            response.sendRedirect(request.getContextPath() + "/alterar-senha-inicial");
            return false;
        }

        return true;
    }
}
