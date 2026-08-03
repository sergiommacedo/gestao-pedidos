package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.ficha.*;
import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.*;
import br.com.sergio.gestaopedidos.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FichaTecnicaService {
    private final FichaTecnicaRepository fichaRepository;
    private final ProdutoService produtoService;
    private final InsumoService insumoService;
    private final SaldoEstoqueRepository saldoRepository;

    @Transactional(readOnly = true)
    public Page<FichaTecnicaResponse> listar(String produto, Boolean ativa, String situacaoCusto, Pageable pageable) {
        String busca = produto == null ? "" : produto.trim();
        String custo = Set.of("COMPLETO", "PENDENTE").contains(situacaoCusto) ? situacaoCusto : "";
        Page<FichaTecnica> pagina = fichaRepository.listar(busca, ativa, custo, pageable);
        if (pagina.isEmpty()) return new PageImpl<>(List.of(), pageable, pagina.getTotalElements());
        List<Long> ids = pagina.stream().map(FichaTecnica::getId).toList();
        Map<Long, FichaTecnica> detalhadas = fichaRepository.buscarDetalhadas(ids).stream()
                .collect(Collectors.toMap(FichaTecnica::getId, Function.identity()));
        List<FichaTecnica> ordenadas = ids.stream().map(detalhadas::get).filter(Objects::nonNull).toList();
        Map<Long, SaldoEstoque> saldos = buscarSaldos(ordenadas);
        return new PageImpl<>(ordenadas.stream().map(f -> mapear(f, saldos)).toList(), pageable, pagina.getTotalElements());
    }

    @Transactional(readOnly = true)
    public FichaTecnicaResponse buscarPorId(Long id) {
        FichaTecnica ficha = buscarDetalhada(id);
        return mapear(ficha, buscarSaldos(List.of(ficha)));
    }

    @Transactional(readOnly = true)
    public Optional<FichaTecnicaResponse> buscarPorProduto(Long produtoId) {
        return fichaRepository.findByProdutoId(produtoId).map(f -> buscarPorId(f.getId()));
    }

    @Transactional(readOnly = true)
    public List<InsumoFichaCustoResponse> buscarInsumos(String termo) {
        return saldoRepository.buscarInsumosAtivosComCusto(termo == null ? "" : termo.trim()).stream()
                .map(this::mapearCusto).toList();
    }

    @Transactional(readOnly = true)
    public InsumoFichaCustoResponse buscarCustoInsumo(Long id) {
        Insumo insumo = insumoService.buscarEntidade(id);
        SaldoEstoque saldo = saldoRepository.buscarSaldo(TipoItemEstoque.INSUMO, id).orElse(null);
        return custo(insumo.getId(), insumo.getNome(), insumo.getUnidadeMedida(), saldo);
    }

    public FichaTecnicaResponse salvar(FichaTecnicaRequest request) {
        Produto produto = validarProdutoNovo(request.getProdutoId());
        if (fichaRepository.existsByProdutoId(produto.getId()))
            throw new BusinessException("Este produto já possui uma ficha técnica.");
        FichaTecnica ficha = FichaTecnica.builder().produto(produto).observacao(texto(request.getObservacao()))
                .ativa(request.getAtiva() == null ? true : request.getAtiva()).build();
        montarItensNovos(ficha, request.getItens());
        fichaRepository.save(ficha);
        return buscarPorId(ficha.getId());
    }

    public FichaTecnicaResponse atualizar(Long id, FichaTecnicaRequest request) {
        FichaTecnica ficha = buscarDetalhada(id);
        if (request.getProdutoId() == null || !ficha.getProduto().getId().equals(request.getProdutoId()))
            throw new BusinessException("O produto da ficha técnica não pode ser alterado.");
        validarLista(request.getItens());
        Map<Long, ItemFichaTecnica> existentes = ficha.getItens().stream()
                .collect(Collectors.toMap(ItemFichaTecnica::getId, Function.identity()));
        Set<Long> idsRecebidos = new HashSet<>();
        Set<Long> insumosRecebidos = new HashSet<>();
        List<ItemFichaTecnica> resultado = new ArrayList<>();
        for (ItemFichaTecnicaRequest itemRequest : request.getItens()) {
            if (!insumosRecebidos.add(itemRequest.getInsumoId())) duplicado();
            ItemFichaTecnica item;
            if (itemRequest.getId() != null) {
                if (!idsRecebidos.add(itemRequest.getId()))
                    throw new BusinessException("Um item da ficha foi informado mais de uma vez.");
                item = existentes.get(itemRequest.getId());
                if (item == null) throw new BusinessException("Item informado não pertence a esta ficha técnica.");
                if (!item.getInsumo().getId().equals(itemRequest.getInsumoId())) {
                    Insumo novo = validarInsumoNovo(itemRequest.getInsumoId());
                    aplicarInsumo(item, novo);
                }
            } else {
                Insumo insumo = validarInsumoNovo(itemRequest.getInsumoId());
                item = ItemFichaTecnica.builder().build();
                aplicarInsumo(item, insumo);
            }
            item.setQuantidade(validarQuantidade(itemRequest.getQuantidade(), item.getUnidadeHistorica()));
            item.setFichaTecnica(ficha);
            resultado.add(item);
        }
        ficha.getItens().clear();
        ficha.getItens().addAll(resultado);
        ficha.setObservacao(texto(request.getObservacao()));
        ficha.setAtiva(request.getAtiva() == null ? ficha.getAtiva() : request.getAtiva());
        fichaRepository.save(ficha);
        return buscarPorId(id);
    }

    public void ativar(Long id) { buscarDetalhada(id).setAtiva(true); }
    public void inativar(Long id) { buscarDetalhada(id).setAtiva(false); }
    public void excluir(Long id) { fichaRepository.delete(buscarDetalhada(id)); }

    private void montarItensNovos(FichaTecnica ficha, List<ItemFichaTecnicaRequest> requests) {
        validarLista(requests);
        Set<Long> ids = new HashSet<>();
        for (ItemFichaTecnicaRequest request : requests) {
            if (!ids.add(request.getInsumoId())) duplicado();
            Insumo insumo = validarInsumoNovo(request.getInsumoId());
            ItemFichaTecnica item = ItemFichaTecnica.builder()
                    .insumo(insumo).nomeHistorico(insumo.getNome()).unidadeHistorica(insumo.getUnidadeMedida())
                    .quantidade(validarQuantidade(request.getQuantidade(), insumo.getUnidadeMedida())).build();
            ficha.adicionarItem(item);
        }
    }

    private Produto validarProdutoNovo(Long id) {
        if (id == null) throw new BusinessException("Selecione o produto.");
        Produto produto = produtoService.buscarEntidadePorId(id);
        if (produto.getTipoProduto() != TipoProduto.PRODUZIDO)
            throw new BusinessException("Somente produtos produzidos podem possuir ficha técnica.");
        if (!Boolean.TRUE.equals(produto.getAtivo()))
            throw new BusinessException("Este produto não está disponível para uma nova ficha técnica.");
        return produto;
    }

    private Insumo validarInsumoNovo(Long id) {
        if (id == null) throw new BusinessException("Selecione o insumo.");
        Insumo insumo = insumoService.buscarEntidade(id);
        if (!Boolean.TRUE.equals(insumo.getAtivo()))
            throw new BusinessException("Este insumo não está disponível para uma nova ficha técnica.");
        return insumo;
    }

    private void validarLista(List<ItemFichaTecnicaRequest> itens) {
        if (itens == null || itens.isEmpty())
            throw new BusinessException("Adicione ao menos um insumo à ficha técnica.");
    }

    private BigDecimal validarQuantidade(BigDecimal quantidade, UnidadeMedida unidade) {
        if (quantidade == null || quantidade.signum() <= 0)
            throw new BusinessException("A quantidade deve ser maior que zero.");
        if (quantidade.stripTrailingZeros().scale() > 3)
            throw new BusinessException("A quantidade deve ter no máximo três casas decimais.");
        if (unidade == UnidadeMedida.UNIDADE && quantidade.stripTrailingZeros().scale() > 0)
            throw new BusinessException("Quantidade em unidade deve ser um número inteiro.");
        return quantidade.setScale(3, RoundingMode.UNNECESSARY);
    }

    private void aplicarInsumo(ItemFichaTecnica item, Insumo insumo) {
        item.setInsumo(insumo); item.setNomeHistorico(insumo.getNome());
        item.setUnidadeHistorica(insumo.getUnidadeMedida());
    }

    private FichaTecnica buscarDetalhada(Long id) {
        return fichaRepository.buscarDetalhada(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ficha técnica não encontrada."));
    }

    private Map<Long, SaldoEstoque> buscarSaldos(Collection<FichaTecnica> fichas) {
        Set<Long> ids = fichas.stream().flatMap(f -> f.getItens().stream())
                .map(i -> i.getInsumo().getId()).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return saldoRepository.buscarSaldosInsumos(ids).stream()
                .collect(Collectors.toMap(s -> s.getInsumo().getId(), Function.identity()));
    }

    private FichaTecnicaResponse mapear(FichaTecnica ficha, Map<Long, SaldoEstoque> saldos) {
        int pendentes = 0;
        BigDecimal total = BigDecimal.ZERO;
        List<ItemFichaTecnicaResponse> itens = new ArrayList<>();
        for (ItemFichaTecnica item : ficha.getItens()) {
            SaldoEstoque saldo = saldos.get(item.getInsumo().getId());
            BigDecimal medio = saldo == null ? BigDecimal.ZERO.setScale(6) : saldo.getCustoMedioAtual();
            BigDecimal estoque = saldo == null ? BigDecimal.ZERO.setScale(3) : saldo.getQuantidadeAtual();
            boolean possui = medio != null && medio.signum() > 0;
            BigDecimal estimado = possui ? item.getQuantidade().multiply(medio).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2);
            if (!possui) pendentes++;
            total = total.add(estimado);
            itens.add(ItemFichaTecnicaResponse.builder().id(item.getId()).insumoId(item.getInsumo().getId())
                    .insumoNome(item.getNomeHistorico()).unidadeMedida(item.getUnidadeHistorica())
                    .quantidade(item.getQuantidade()).estoqueAtual(estoque).custoMedioAtual(medio)
                    .custoEstimado(estimado).possuiCusto(possui).build());
        }
        total = total.setScale(2, RoundingMode.HALF_UP);
        BigDecimal preco = ficha.getProduto().getPreco() == null ? BigDecimal.ZERO : ficha.getProduto().getPreco();
        BigDecimal margem = preco.subtract(total).setScale(2, RoundingMode.HALF_UP);
        BigDecimal percentual = preco.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                : margem.divide(preco, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        return FichaTecnicaResponse.builder().id(ficha.getId()).produtoId(ficha.getProduto().getId())
                .produtoNome(ficha.getProduto().getNome()).unidadeVendaProduto(ficha.getProduto().getUnidadeVenda())
                .precoVenda(preco).observacao(ficha.getObservacao()).ativa(ficha.getAtiva())
                .criadoEm(ficha.getCriadoEm()).atualizadoEm(ficha.getAtualizadoEm()).itens(itens)
                .custoEstimadoTotal(total).custoCompleto(pendentes == 0).quantidadeItensSemCusto(pendentes)
                .margemContribuicaoEstimada(margem).margemPercentualEstimada(percentual).build();
    }

    private InsumoFichaCustoResponse mapearCusto(SaldoEstoqueRepository.InsumoCusto p) {
        BigDecimal medio = p.getCustoMedio() == null ? BigDecimal.ZERO.setScale(6) : p.getCustoMedio();
        return InsumoFichaCustoResponse.builder().id(p.getId()).nome(p.getNome())
                .unidade(UnidadeMedida.valueOf(p.getUnidade())).custoMedio(medio)
                .estoqueAtual(p.getEstoqueAtual() == null ? BigDecimal.ZERO.setScale(3) : p.getEstoqueAtual())
                .possuiCusto(medio.signum() > 0).build();
    }

    private InsumoFichaCustoResponse custo(Long id, String nome, UnidadeMedida unidade, SaldoEstoque saldo) {
        BigDecimal medio = saldo == null ? BigDecimal.ZERO.setScale(6) : saldo.getCustoMedioAtual();
        return InsumoFichaCustoResponse.builder().id(id).nome(nome).unidade(unidade).custoMedio(medio)
                .estoqueAtual(saldo == null ? BigDecimal.ZERO.setScale(3) : saldo.getQuantidadeAtual())
                .possuiCusto(medio.signum() > 0).build();
    }

    private String texto(String valor) { return valor == null || valor.trim().isEmpty() ? null : valor.trim(); }
    private void duplicado() { throw new BusinessException("Este insumo já foi adicionado à ficha técnica."); }
}
