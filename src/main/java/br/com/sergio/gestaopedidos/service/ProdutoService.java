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
    private final jakarta.persistence.EntityManager entityManager;

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
        return listarPaginado(filtro,null,null,null,pageable);
    }

    @Transactional(readOnly=true)
    public Page<ProdutoResponse> listarPaginado(String filtro,TipoProduto tipo,Boolean ativo,Boolean vendavel,Pageable pageable){
        return produtoRepository.listar(filtro==null?"":filtro.trim(),tipo,ativo,vendavel,pageable).map(produtoMapper::toResponse);
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
                .buscarVendaveis(java.util.EnumSet.of(TipoProduto.PRODUTO_COMERCIAL,TipoProduto.PRODUTO_REVENDA),termoTratado,org.springframework.data.domain.PageRequest.of(0,20))
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

        aplicarRegrasPorTipo(produto,request);
        produto.setEstoqueMinimo(normalizarEstoqueMinimo(request.tipoProduto(), request.unidadeVenda(), request.estoqueMinimo()));

        Produto produtoSalvo = produtoRepository.save(produto);

        return produtoMapper.toResponse(produtoSalvo);
    }

    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarEntidadePorId(id);

        if(entityManager!=null&&produto.getTipoProduto()!=request.tipoProduto())validarMudancaTipo(id);

        validarNomeDuplicadoNaAtualizacao(produto, request.nome());

        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        if (request.ativo() != null) {
            produto.setAtivo(request.ativo());
        }

        aplicarRegrasPorTipo(produto,request);
        produto.setEstoqueMinimo(normalizarEstoqueMinimo(request.tipoProduto(), request.unidadeVenda(), request.estoqueMinimo()));

        Produto produtoAtualizado = produtoRepository.save(produto);

        return produtoMapper.toResponse(produtoAtualizado);
    }

    private void validarMudancaTipo(Long id){Long dependencias=entityManager.createQuery("SELECT COUNT(f) FROM FichaTecnica f WHERE f.produto.id=:id",Long.class).setParameter("id",id).getSingleResult()+entityManager.createQuery("SELECT COUNT(c) FROM ComposicaoProduto c WHERE c.produtoComercial.id=:id",Long.class).setParameter("id",id).getSingleResult()+entityManager.createQuery("SELECT COUNT(s) FROM SaldoEstoque s WHERE s.produto.id=:id",Long.class).setParameter("id",id).getSingleResult()+entityManager.createQuery("SELECT COUNT(i) FROM ItemCompra i WHERE i.produto.id=:id",Long.class).setParameter("id",id).getSingleResult()+entityManager.createQuery("SELECT COUNT(i) FROM ItemProducao i WHERE i.produto.id=:id",Long.class).setParameter("id",id).getSingleResult();if(dependencias>0)throw new BusinessException("O tipo do produto não pode ser alterado porque existem Fichas, Composições, Estoque, Compras ou Produções vinculadas.");}

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
        return produtoRepository.findByTipoProdutoOrderByNomeAsc(TipoProduto.PREPARACAO_PRODUZIDA).stream().map(produtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listarProduzidosAtivos() {
        return produtoRepository.findByTipoProdutoAndAtivoTrueOrderByNomeAsc(TipoProduto.PREPARACAO_PRODUZIDA).stream().map(produtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> buscarRevendaAtivosPorNome(String termo) {
        return produtoRepository.findTop20ByTipoProdutoAndAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(
                TipoProduto.PRODUTO_REVENDA, termo == null ? "" : termo.trim()).stream().map(produtoMapper::toResponse).toList();
    }

    @Transactional(readOnly=true)
    public List<ProdutoResponse> listarComerciaisAtivos(){return produtoRepository.findByTipoProdutoAndAtivoTrueOrderByNomeAsc(TipoProduto.PRODUTO_COMERCIAL).stream().map(produtoMapper::toResponse).toList();}

    private void aplicarRegrasPorTipo(Produto produto,ProdutoRequest request){
        TipoProduto tipo=request.tipoProduto();
        if(tipo==null)throw new BusinessException("Tipo do produto é obrigatório.");
        if(request.unidadeVenda()==null)throw new BusinessException("Unidade é obrigatória.");
        produto.setTipoProduto(tipo);produto.setUnidadeVenda(request.unidadeVenda());
        if(tipo==TipoProduto.PREPARACAO_PRODUZIDA){
            produto.setPreco(null);produto.setVendavel(false);produto.setPermiteAcompanhamento(false);
        }else{
            if(request.preco()==null||request.preco().signum()<=0)throw new BusinessException("Preço deve ser maior que zero.");
            produto.setPreco(request.preco().setScale(2,java.math.RoundingMode.HALF_UP));
            produto.setVendavel(Boolean.TRUE.equals(request.vendavel()));
            produto.setPermiteAcompanhamento(Boolean.TRUE.equals(request.permiteAcompanhamento()));
        }
    }

    private void validarNomeDuplicado(String nome) {
        if (produtoRepository.existsByNomeIgnoreCase(nome)) {
            throw new BusinessException(
                    "Já existe um produto cadastrado com esse nome."
            );
        }
    }

    private BigDecimal normalizarEstoqueMinimo(TipoProduto tipo, UnidadeVenda unidade, BigDecimal valor) {
        if (tipo == TipoProduto.PRODUTO_COMERCIAL) return BigDecimal.ZERO.setScale(3);
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
