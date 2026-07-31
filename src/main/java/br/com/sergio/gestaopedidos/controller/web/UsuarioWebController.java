package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.usuario.UsuarioResponse;
import br.com.sergio.gestaopedidos.dto.usuario.UsuarioWebForm;
import br.com.sergio.gestaopedidos.enums.PerfilUsuario;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import jakarta.validation.Valid;
import br.com.sergio.gestaopedidos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioWebController {

    private final UsuarioService usuarioService;

    @ModelAttribute("perfis")
    public PerfilUsuario[] perfis() {
        return PerfilUsuario.values();
    }

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            Model model
    ) {
        int paginaValida = Math.max(pagina, 0);
        int tamanhoValido = validarTamanhoPagina(tamanho);
        String filtroTratado = filtro == null ? "" : filtro.trim();

        PageRequest pageable = PageRequest.of(
                paginaValida,
                tamanhoValido,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<UsuarioResponse> paginaUsuarios =
                usuarioService.listarPaginado(filtroTratado, pageable);

        model.addAttribute("paginaUsuarios", paginaUsuarios);
        model.addAttribute("usuarios", paginaUsuarios.getContent());
        model.addAttribute("filtro", filtroTratado);
        model.addAttribute("tamanho", tamanhoValido);

        return "usuarios/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("usuario", UsuarioWebForm.builder()
                .ativo(true)
                .trocarSenhaPrimeiroAcesso(false)
                .build());
        prepararFormulario(model, "Novo usuário", false, null);
        return "usuarios/formulario";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("usuario") UsuarioWebForm usuario,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validarSenha(usuario, bindingResult, true);

        if (bindingResult.hasErrors()) {
            prepararFormulario(model, "Novo usuário", false, null);
            return "usuarios/formulario";
        }

        try {
            usuarioService.cadastrarWeb(usuario);
        } catch (BusinessException exception) {
            bindingResult.rejectValue("login", "usuario.login.duplicado", exception.getMessage());
            prepararFormulario(model, "Novo usuário", false, null);
            return "usuarios/formulario";
        }

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Usuário cadastrado com sucesso."
        );
        return "redirect:/usuarios";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        UsuarioResponse usuario = usuarioService.buscarPorId(id);

        model.addAttribute("usuario", UsuarioWebForm.builder()
                .nome(usuario.nome())
                .login(usuario.email())
                .perfil(usuario.perfil())
                .ativo(usuario.ativo())
                .trocarSenhaPrimeiroAcesso(usuario.trocarSenhaPrimeiroAcesso())
                .build());
        prepararFormulario(model, "Editar usuário", true, id);
        return "usuarios/formulario";
    }

    @PostMapping("/{id}")
    public String atualizar(
            @PathVariable Long id,
            @Valid @ModelAttribute("usuario") UsuarioWebForm usuario,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        validarSenha(usuario, bindingResult, false);

        if (bindingResult.hasErrors()) {
            prepararFormulario(model, "Editar usuário", true, id);
            return "usuarios/formulario";
        }

        try {
            usuarioService.atualizarWeb(id, usuario, authentication.getName());
        } catch (BusinessException exception) {
            bindingResult.reject("usuario.atualizacao.invalida", exception.getMessage());
            prepararFormulario(model, "Editar usuário", true, id);
            return "usuarios/formulario";
        }

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Usuário atualizado com sucesso."
        );
        return "redirect:/usuarios";
    }

    private void validarSenha(
            UsuarioWebForm usuario,
            BindingResult bindingResult,
            boolean cadastro
    ) {
        boolean senhaInformada = usuario.getSenha() != null && !usuario.getSenha().isBlank();
        boolean confirmacaoInformada = usuario.getConfirmarSenha() != null
                && !usuario.getConfirmarSenha().isBlank();

        if (cadastro && !senhaInformada) {
            bindingResult.rejectValue("senha", "usuario.senha.obrigatoria", "Senha é obrigatória.");
            return;
        }

        if (!senhaInformada && confirmacaoInformada) {
            bindingResult.rejectValue("senha", "usuario.senha.obrigatoria", "Informe a nova senha.");
            return;
        }

        if (!senhaInformada) {
            return;
        }

        if (usuario.getSenha().length() < 6) {
            bindingResult.rejectValue(
                    "senha",
                    "usuario.senha.tamanho",
                    "Senha deve ter no mínimo 6 caracteres."
            );
        }

        if (!usuario.getSenha().equals(usuario.getConfirmarSenha())) {
            bindingResult.rejectValue(
                    "confirmarSenha",
                    "usuario.senha.confirmacao",
                    "A confirmação da senha não coincide."
            );
        }
    }

    private void prepararFormulario(
            Model model,
            String titulo,
            boolean modoEdicao,
            Long usuarioId
    ) {
        model.addAttribute("titulo", titulo);
        model.addAttribute("modoEdicao", modoEdicao);
        if (usuarioId != null) {
            model.addAttribute("usuarioId", usuarioId);
        }
    }

    private int validarTamanhoPagina(int tamanho) {
        return tamanho == 10 || tamanho == 20 || tamanho == 50 ? tamanho : 10;
    }
}
