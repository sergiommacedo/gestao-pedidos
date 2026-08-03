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
import br.com.sergio.gestaopedidos.enums.TipoProduto;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import java.math.BigDecimal;

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

    @Transactional(readOnly = true)
    public List<ProdutoResponse> buscarAtivosEVendaveisPorNome(String termo) {
        String termoTratado = termo == null ? "" : termo.trim();

        return produtoRepository
                .findTop20ByAtivoTrueAndVendavelTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(
                        termoTratado
                )
                .stream()
                .map(produtoMapper::toResponse)
                .toList();
    }

    public ProdutoResponse salvar(ProdutoRequest request) {
        validarNomeDuplicado(request.nome());

        Produto produto = produtoMapper.toEntity(request);

        if (produto.getAtivo() == null) {
            produto.setAtivo(true);
        }

        produto.setTipoProduto(request.tipoProduto() == null ? TipoProduto.PRODUZIDO : request.tipoProduto());
        produto.setVendavel(request.vendavel() == null ? true : request.vendavel());
        produto.setEstoqueMinimo(normalizarEstoqueMinimo(request.tipoProduto(), request.unidadeVenda(), request.estoqueMinimo()));

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
        produto.setTipoProduto(request.tipoProduto() == null ? TipoProduto.PRODUZIDO : request.tipoProduto());
        produto.setVendavel(request.vendavel() == null ? true : request.vendavel());
        produto.setEstoqueMinimo(normalizarEstoqueMinimo(request.tipoProduto(), request.unidadeVenda(), request.estoqueMinimo()));

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

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarAtivos() {
        return produtoRepository.findByAtivoTrueOrderByNomeAsc().stream().map(produtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarProduzidos() {
        return produtoRepository.findByTipoProdutoOrderByNomeAsc(TipoProduto.PRODUZIDO).stream().map(produtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarProduzidosAtivos() {
        return produtoRepository.findByTipoProdutoAndAtivoTrueOrderByNomeAsc(TipoProduto.PRODUZIDO).stream().map(produtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> buscarRevendaAtivosPorNome(String termo) {
        return produtoRepository.findTop20ByTipoProdutoAndAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(
                TipoProduto.REVENDA, termo == null ? "" : termo.trim()).stream().map(produtoMapper::toResponse).toList();
    }

    private void validarNomeDuplicado(String nome) {
        if (produtoRepository.existsByNomeIgnoreCase(nome)) {
            throw new BusinessException(
                    "Já existe um produto cadastrado com esse nome."
            );
        }
    }

    private BigDecimal normalizarEstoqueMinimo(TipoProduto tipo, UnidadeVenda unidade, BigDecimal valor) {
        if (tipo != TipoProduto.REVENDA) return BigDecimal.ZERO.setScale(3);
        BigDecimal minimo = valor == null ? BigDecimal.ZERO : valor;
        if (minimo.signum() < 0 || minimo.stripTrailingZeros().scale() > 3)
            throw new BusinessException("Estoque mínimo deve ser positivo e ter no máximo três casas decimais.");
        if (unidade == UnidadeVenda.UNIDADE && minimo.stripTrailingZeros().scale() > 0)
            throw new BusinessException("Estoque mínimo em unidade deve ser inteiro.");
        return minimo.setScale(3);
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
