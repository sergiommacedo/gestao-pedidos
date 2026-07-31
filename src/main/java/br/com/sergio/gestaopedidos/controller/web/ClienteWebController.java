package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.cliente.ClienteRequest;
import br.com.sergio.gestaopedidos.dto.cliente.ClienteResponse;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteWebController {

    private static final Set<String> CAMPOS_ORDENACAO =
            Set.of(
                    "id",
                    "nome",
                    "telefone",
                    "cidade"
            );

    private final ClienteService clienteService;

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "") String filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            @RequestParam(defaultValue = "id") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direcao,
            Model model
    ) {
        String campoOrdenacao =
                validarCampoOrdenacao(ordenarPor);

        Sort.Direction direcaoOrdenacao =
                converterDirecao(direcao);

        int paginaValida = Math.max(pagina, 0);
        int tamanhoValido = validarTamanhoPagina(tamanho);

        PageRequest pageable = PageRequest.of(
                paginaValida,
                tamanhoValido,
                Sort.by(direcaoOrdenacao, campoOrdenacao)
        );

        String filtroTratado =
                filtro == null ? "" : filtro.trim();

        Page<ClienteResponse> paginaClientes =
                clienteService.listarPaginado(
                        filtroTratado,
                        pageable
                );

        model.addAttribute(
                "paginaClientes",
                paginaClientes
        );

        model.addAttribute(
                "clientes",
                paginaClientes.getContent()
        );

        model.addAttribute(
                "filtro",
                filtroTratado
        );

        model.addAttribute(
                "ordenarPor",
                campoOrdenacao
        );

        model.addAttribute(
                "direcao",
                direcaoOrdenacao
                        .name()
                        .toLowerCase()
        );

        model.addAttribute(
                "tamanho",
                tamanhoValido
        );

        return "clientes/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute(
                "cliente",
                ClienteRequest.builder().build()
        );

        model.addAttribute(
                "titulo",
                "Novo Cliente"
        );

        model.addAttribute(
                "modoEdicao",
                false
        );

        return "clientes/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {
        ClienteResponse cliente =
                clienteService.buscarPorId(id);

        ClienteRequest clienteRequest =
                ClienteRequest.builder()
                        .nome(cliente.nome())
                        .telefone(cliente.telefone())
                        .endereco(cliente.endereco())
                        .numero(cliente.numero())
                        .bairro(cliente.bairro())
                        .cidade(cliente.cidade())
                        .cep(cliente.cep())
                        .complemento(cliente.complemento())
                        .build();

        model.addAttribute(
                "cliente",
                clienteRequest
        );

        model.addAttribute(
                "clienteId",
                id
        );

        model.addAttribute(
                "titulo",
                "Editar Cliente"
        );

        model.addAttribute(
                "modoEdicao",
                true
        );

        return "clientes/formulario";
    }

    @PostMapping
    public String salvar(
            @Valid
            @ModelAttribute("cliente")
            ClienteRequest cliente,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "titulo",
                    "Novo Cliente"
            );

            model.addAttribute(
                    "modoEdicao",
                    false
            );

            return "clientes/formulario";
        }

        clienteService.salvar(cliente);

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Cliente cadastrado com sucesso."
        );

        return "redirect:/clientes";
    }

    @PostMapping("/{id}")
    public String atualizar(
            @PathVariable Long id,
            @Valid
            @ModelAttribute("cliente")
            ClienteRequest cliente,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute(
                    "clienteId",
                    id
            );

            model.addAttribute(
                    "titulo",
                    "Editar Cliente"
            );

            model.addAttribute(
                    "modoEdicao",
                    true
            );

            return "clientes/formulario";
        }

        clienteService.atualizar(id, cliente);

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Cliente atualizado com sucesso."
        );

        return "redirect:/clientes";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        try {
            clienteService.excluir(id);

            redirectAttributes.addFlashAttribute(
                    "mensagemSucesso",
                    "Cliente excluído com sucesso."
            );
        } catch (BusinessException exception) {
            redirectAttributes.addFlashAttribute(
                    "mensagemErro",
                    exception.getMessage()
            );
        }

        return "redirect:/clientes";
    }

    private String validarCampoOrdenacao(
            String ordenarPor
    ) {
        if (CAMPOS_ORDENACAO.contains(ordenarPor)) {
            return ordenarPor;
        }

        return "id";
    }

    private Sort.Direction converterDirecao(
            String direcao
    ) {
        if ("asc".equalsIgnoreCase(direcao)) {
            return Sort.Direction.ASC;
        }

        return Sort.Direction.DESC;
    }

    private int validarTamanhoPagina(
            int tamanho
    ) {
        if (tamanho == 10 ||
                tamanho == 20 ||
                tamanho == 50) {
            return tamanho;
        }

        return 10;
    }
}
