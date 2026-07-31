package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.produto.ProdutoRequest;
import br.com.sergio.gestaopedidos.dto.produto.ProdutoResponse;
import br.com.sergio.gestaopedidos.entity.Produto;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import br.com.sergio.gestaopedidos.mapper.ProdutoMapper;
import br.com.sergio.gestaopedidos.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final ProdutoMapper produtoMapper;

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarTodos() {
        return produtoRepository.findAll()
                .stream()
                .map(produtoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listarPaginado(
            String filtro,
            Pageable pageable
    ) {
        Page<Produto> paginaProdutos;

        if (filtro == null || filtro.isBlank()) {
            paginaProdutos = produtoRepository.findAll(pageable);
        } else {
            String filtroTratado = filtro.trim();

            paginaProdutos = produtoRepository
                    .findByNomeContainingIgnoreCaseOrDescricaoContainingIgnoreCase(
                            filtroTratado,
                            filtroTratado,
                            pageable
                    );
        }

        return paginaProdutos.map(produtoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        Produto produto = buscarEntidadePorId(id);
        return produtoMapper.toResponse(produto);
    }

    public ProdutoResponse salvar(ProdutoRequest request) {
        validarNomeDuplicado(request.nome());

        Produto produto = produtoMapper.toEntity(request);

        if (produto.getAtivo() == null) {
            produto.setAtivo(true);
        }

        Produto produtoSalvo = produtoRepository.save(produto);

        return produtoMapper.toResponse(produtoSalvo);
    }

    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidadePorId(id);

        validarNomeDuplicadoNaAtualizacao(produto, request.nome());

        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPreco(request.preco());

        if (request.ativo() != null) {
            produto.setAtivo(request.ativo());
        }

        produto.setUnidadeVenda(request.unidadeVenda());
        produto.setPermiteAcompanhamento(request.permiteAcompanhamento());

        Produto produtoAtualizado = produtoRepository.save(produto);

        return produtoMapper.toResponse(produtoAtualizado);
    }

    public void excluir(Long id) {
        Produto produto = buscarEntidadePorId(id);
        produtoRepository.delete(produto);
    }

    public void ativar(Long id) {
        Produto produto = buscarEntidadePorId(id);
        produto.setAtivo(true);
    }

    public void inativar(Long id) {
        Produto produto = buscarEntidadePorId(id);
        produto.setAtivo(false);
    }

    @Transactional(readOnly = true)
    public Produto buscarEntidadePorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Produto não encontrado."
                        )
                );
    }

    private void validarNomeDuplicado(String nome) {
        if (produtoRepository.existsByNomeIgnoreCase(nome)) {
            throw new BusinessException(
                    "Já existe um produto cadastrado com esse nome."
            );
        }
    }

    private void validarNomeDuplicadoNaAtualizacao(
            Produto produto,
            String novoNome
    ) {
        boolean nomeFoiAlterado =
                !produto.getNome().equalsIgnoreCase(novoNome);

        if (nomeFoiAlterado
                && produtoRepository.existsByNomeIgnoreCase(novoNome)) {
            throw new BusinessException(
                    "Já existe um produto cadastrado com esse nome."
            );
        }
    }
}
