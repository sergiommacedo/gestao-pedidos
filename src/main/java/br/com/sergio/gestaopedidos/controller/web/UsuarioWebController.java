package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.usuario.UsuarioResponse;
import br.com.sergio.gestaopedidos.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioWebController {

    private final UsuarioService usuarioService;

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

    private int validarTamanhoPagina(int tamanho) {
        return tamanho == 10 || tamanho == 20 || tamanho == 50 ? tamanho : 10;
    }
}
