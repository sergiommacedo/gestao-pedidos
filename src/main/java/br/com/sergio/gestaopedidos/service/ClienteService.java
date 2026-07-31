package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.cliente.ClienteRequest;
import br.com.sergio.gestaopedidos.dto.cliente.ClienteResponse;
import br.com.sergio.gestaopedidos.entity.Cliente;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import br.com.sergio.gestaopedidos.mapper.ClienteMapper;
import br.com.sergio.gestaopedidos.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodos() {
        return clienteRepository.findAll()
                .stream()
                .map(clienteMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listarPaginado(
            String filtro,
            Pageable pageable
    ) {
        Page<Cliente> paginaClientes;

        if (filtro == null || filtro.isBlank()) {
            paginaClientes = clienteRepository.findAll(pageable);
        } else {
            String filtroTratado = filtro.trim();

            paginaClientes =
                    clienteRepository
                            .findByNomeContainingIgnoreCaseOrTelefoneContainingIgnoreCase(
                                    filtroTratado,
                                    filtroTratado,
                                    pageable
                            );
        }

        return paginaClientes.map(clienteMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        Cliente cliente = buscarEntidadePorId(id);

        return clienteMapper.toResponse(cliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarPorNomeOuTelefone(String termo) {
        if (termo == null || termo.isBlank()) {
            return List.of();
        }

        String termoTratado = termo.trim();

        return clienteRepository
                .findByNomeContainingIgnoreCaseOrTelefoneContainingIgnoreCase(
                        termoTratado,
                        termoTratado,
                        PageRequest.of(0, 10)
                )
                .getContent()
                .stream()
                .map(clienteMapper::toResponse)
                .toList();
    }

    public ClienteResponse salvar(ClienteRequest request) {
        Cliente cliente = clienteMapper.toEntity(request);

        Cliente clienteSalvo = clienteRepository.save(cliente);

        return clienteMapper.toResponse(clienteSalvo);
    }

    public ClienteResponse atualizar(
            Long id,
            ClienteRequest request
    ) {
        Cliente cliente = buscarEntidadePorId(id);

        cliente.setNome(request.nome());
        cliente.setTelefone(request.telefone());
        cliente.setEndereco(request.endereco());
        cliente.setNumero(request.numero());
        cliente.setBairro(request.bairro());
        cliente.setCidade(request.cidade());
        cliente.setCep(request.cep());
        cliente.setComplemento(request.complemento());

        Cliente clienteAtualizado =
                clienteRepository.save(cliente);

        return clienteMapper.toResponse(clienteAtualizado);
    }

    public void excluir(Long id) {
        Cliente cliente = buscarEntidadePorId(id);

        clienteRepository.delete(cliente);
    }

    private Cliente buscarEntidadePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cliente não encontrado."
                        )
                );
    }
}
