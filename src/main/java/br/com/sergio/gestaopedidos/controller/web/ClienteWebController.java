package br.com.sergio.gestaopedidos.controller.web;

import br.com.sergio.gestaopedidos.dto.cliente.ClienteRequest;
import br.com.sergio.gestaopedidos.dto.cliente.ClienteResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteWebController {

    private final ClienteService clienteService;

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            Model model
    ) {
        PageRequest pageable = PageRequest.of(
                pagina,
                tamanho,
                Sort.by("id").descending()
        );

        Page<ClienteResponse> paginaClientes =
                clienteService.listarPaginado(pageable);

        model.addAttribute("paginaClientes", paginaClientes);
        model.addAttribute("clientes", paginaClientes.getContent());

        return "clientes/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute(
                "cliente",
                ClienteRequest.builder().build()
        );

        model.addAttribute("titulo", "Novo Cliente");
        model.addAttribute("modoEdicao", false);

        return "clientes/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(
            @PathVariable Long id,
            Model model
    ) {
        ClienteResponse cliente = clienteService.buscarPorId(id);

        ClienteRequest clienteRequest = ClienteRequest.builder()
                .nome(cliente.nome())
                .telefone(cliente.telefone())
                .endereco(cliente.endereco())
                .numero(cliente.numero())
                .bairro(cliente.bairro())
                .cidade(cliente.cidade())
                .cep(cliente.cep())
                .complemento(cliente.complemento())
                .build();

        model.addAttribute("cliente", clienteRequest);
        model.addAttribute("clienteId", id);
        model.addAttribute("titulo", "Editar Cliente");
        model.addAttribute("modoEdicao", true);

        return "clientes/formulario";
    }

    @PostMapping
    public String salvar(
            @Valid @ModelAttribute("cliente") ClienteRequest cliente,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("titulo", "Novo Cliente");
            model.addAttribute("modoEdicao", false);

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
            @Valid @ModelAttribute("cliente") ClienteRequest cliente,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("clienteId", id);
            model.addAttribute("titulo", "Editar Cliente");
            model.addAttribute("modoEdicao", true);

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

        clienteService.excluir(id);

        redirectAttributes.addFlashAttribute(
                "mensagemSucesso",
                "Cliente excluído com sucesso."
        );

        return "redirect:/clientes";
    }
}