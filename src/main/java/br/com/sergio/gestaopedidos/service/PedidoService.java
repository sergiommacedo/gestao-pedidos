package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.pedido.ItemPedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.DashboardOperacionalResponse;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.dto.pedido.PreviaEstoquePedidoResponse;
import br.com.sergio.gestaopedidos.entity.Cliente;
import br.com.sergio.gestaopedidos.entity.ItemPedido;
import br.com.sergio.gestaopedidos.entity.Pedido;
import br.com.sergio.gestaopedidos.entity.Produto;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import br.com.sergio.gestaopedidos.mapper.PedidoMapper;
import br.com.sergio.gestaopedidos.repository.ClienteRepository;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import br.com.sergio.gestaopedidos.repository.ProdutoRepository;
import br.com.sergio.gestaopedidos.repository.MovimentacaoEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {

    private static final Set<StatusPedido> STATUS_EDITAVEIS = Set.copyOf(EnumSet.of(
            StatusPedido.PENDENTE,
            StatusPedido.EM_PREPARACAO
    ));

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoMapper pedidoMapper;
    private final EstoqueService estoqueService;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarTodos() {
        return pedidoRepository.findAll()
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PedidoResponse> listarPaginado(
            String filtro,
            StatusPedido status,
            LocalDate dataAgendada,
            TipoEntrega tipoEntrega,
            br.com.sergio.gestaopedidos.enums.FormaPagamento formaPagamento,
            String situacaoEstoque,
            Pageable pageable
    ) {
        String filtroTratado = filtro == null ? "" : filtro.trim();

        return pedidoRepository.buscarPaginado(
                        filtroTratado,
                        status,
                        dataAgendada,
                        tipoEntrega,
                        formaPagamento,
                        situacaoEstoque == null ? "" : situacaoEstoque,
                        pageable
                )
                .map(pedidoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public DashboardOperacionalResponse buscarDashboardOperacional(LocalDate data) {
        LocalDate dataReferencia = data == null ? LocalDate.now() : data;

        return new DashboardOperacionalResponse(
                dataReferencia,
                pedidoRepository.countByDataAgendada(dataReferencia),
                pedidoRepository.countByDataAgendadaAndStatus(dataReferencia, StatusPedido.EM_PREPARACAO),
                pedidoRepository.countByDataAgendadaAndStatus(
                        dataReferencia,
                        StatusPedido.ENTREGUE
                ),
                pedidoRepository.countByDataAgendadaAndStatus(
                        dataReferencia,
                        StatusPedido.CANCELADO
                ),
                pedidoRepository.somarValorTotalPorDataExcetoStatus(
                        dataReferencia,
                        StatusPedido.CANCELADO
                )
        );
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorData(LocalDate dataAgendada) {
        return pedidoRepository.findByDataAgendadaOrderByDataPedidoAsc(dataAgendada)
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        Pedido pedido = buscarEntidadePorId(id);
        return pedidoMapper.toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> buscarPorIds(List<Long> ids) {
        return pedidoRepository.findAllById(ids)
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarParaEdicao(Long id) {
        Pedido pedido = buscarEntidadePorId(id);
        validarEditavel(pedido);
        return pedidoMapper.toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarParaImpressao(Long id) {
        Pedido pedido = buscarEntidadePorId(id);
        validarImprimivel(pedido);
        return pedidoMapper.toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> buscarPorIdsParaImpressao(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("Selecione ao menos um pedido para impressão.");
        }

        List<Long> idsUnicos = ids.stream().distinct().toList();
        Map<Long, Pedido> pedidosPorId = pedidoRepository.findAllById(idsUnicos)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        Pedido::getId,
                        pedido -> pedido,
                        (primeiro, segundo) -> primeiro,
                        LinkedHashMap::new
                ));

        if (pedidosPorId.size() != idsUnicos.size()) {
            throw new ResourceNotFoundException("Um ou mais pedidos não foram encontrados.");
        }

        List<Pedido> pedidos = idsUnicos.stream().map(pedidosPorId::get).toList();
        if (pedidos.stream().anyMatch(pedido -> !isImprimivel(pedido.getStatus()))) {
            throw new BusinessException(
                    "Um ou mais pedidos selecionados não estão disponíveis para impressão."
            );
        }

        return pedidos.stream().map(pedidoMapper::toResponse).toList();
    }

    public boolean isEditavel(StatusPedido status) {
        return status != null && STATUS_EDITAVEIS.contains(status);
    }

    public boolean isImprimivel(StatusPedido status) {
        return status != null;
    }

    public Set<StatusPedido> statusEditaveis() {
        return STATUS_EDITAVEIS;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> situacoesEstoque(List<PedidoResponse> pedidos) {
        Map<Long, String> resultado = new LinkedHashMap<>();
        for (PedidoResponse pedido : pedidos) resultado.put(pedido.id(), situacaoEstoque(pedido));
        return resultado;
    }

    @Transactional(readOnly = true)
    public String situacaoEstoque(PedidoResponse pedido) {
        if (Boolean.TRUE.equals(pedido.estoqueMovimentado())) return "Estoque baixado";
        if (pedido.status() == StatusPedido.CANCELADO) return movimentacaoEstoqueRepository.existsByPedidoIdAndTipo(pedido.id(), br.com.sergio.gestaopedidos.enums.TipoMovimentacaoEstoque.ESTORNO_SAIDA) ? "Estoque estornado" : "Não se aplica";
        return "Aguardando baixa";
    }

    public PedidoResponse salvar(PedidoRequest request) {
        Cliente cliente = buscarClientePorId(request.clienteId());

        Pedido pedido = criarPedido(request, cliente);

        substituirItensERecalcular(pedido, request);

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        return pedidoMapper.toResponse(pedidoSalvo);
    }

    public PedidoResponse atualizar(
            Long id,
            PedidoRequest request,
            List<Long> itemIds
    ) {
        Pedido pedido = pedidoRepository.bloquearDetalhado(id).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
        validarEditavel(pedido);
        boolean estoqueMovimentado = Boolean.TRUE.equals(pedido.getEstoqueMovimentado());
        if (estoqueMovimentado) estoqueService.estornarPedido(pedido);
        Cliente cliente = buscarClientePorId(request.clienteId());

        pedido.setCliente(cliente);
        pedido.setDataAgendada(request.dataAgendada());
        pedido.setFormaPagamento(request.formaPagamento());
        pedido.setTipoEntrega(request.tipoEntrega());
        pedido.setTaxaEntrega(normalizarTaxaEntrega(request.tipoEntrega(), request.taxaEntrega()));
        pedido.setObservacao(request.observacao());

        atualizarItensERecalcular(pedido, request, itemIds);
        if (estoqueMovimentado) estoqueService.processarPedido(pedido);

        return pedidoMapper.toResponse(pedidoRepository.save(pedido));
    }

    private void validarEditavel(Pedido pedido) {
        if (!isEditavel(pedido.getStatus())) {
            throw new BusinessException("Este pedido não pode mais ser editado.");
        }
    }

    private void validarImprimivel(Pedido pedido) {
        if (!isImprimivel(pedido.getStatus())) {
            throw new BusinessException(
                    "A impressão não está disponível para este pedido."
            );
        }
    }

    public PedidoResponse alterarStatus(
            Long id,
            StatusPedido novoStatus,
            String motivoCancelamento
    ) {
        Pedido pedido = pedidoRepository.bloquearDetalhado(id).orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
        Set<StatusPedido> permitidos = transicoesPermitidas(
                pedido.getStatus(),
                pedido.getTipoEntrega()
        );

        if (novoStatus == null || !permitidos.contains(novoStatus)) {
            throw new BusinessException(
                    "Não é permitido alterar o pedido de "
                            + pedido.getStatus().getDescricao()
                            + " para "
                            + (novoStatus == null
                                ? "um status vazio"
                                : novoStatus.getDescricao())
                            + "."
            );
        }

        if (novoStatus == StatusPedido.CANCELADO) {
            String motivoTratado = motivoCancelamento == null
                    ? ""
                    : motivoCancelamento.trim();

            if (motivoTratado.isEmpty()) {
                throw new BusinessException("Informe o motivo do cancelamento.");
            }

            if (motivoTratado.length() > 500) {
                throw new BusinessException(
                        "O motivo do cancelamento deve possuir no máximo 500 caracteres."
                );
            }

            pedido.setMotivoCancelamento(motivoTratado);
            estoqueService.estornarPedido(pedido);
        } else if (motivoCancelamento != null && !motivoCancelamento.isBlank()) {
            throw new BusinessException(
                    "O motivo do cancelamento só pode ser informado ao cancelar o pedido."
            );
        }

        if (pedido.getStatus() == StatusPedido.PENDENTE && novoStatus == StatusPedido.EM_PREPARACAO) {
            estoqueService.processarPedido(pedido);
        }
        pedido.setStatus(novoStatus);
        return pedidoMapper.toResponse(pedidoRepository.save(pedido));
    }

    public Set<StatusPedido> transicoesPermitidas(
            StatusPedido statusAtual,
            TipoEntrega tipoEntrega
    ) {
        if (statusAtual == null) {
            return Set.of();
        }

        return switch (statusAtual) {
            case PENDENTE -> EnumSet.of(StatusPedido.EM_PREPARACAO, StatusPedido.CANCELADO);
            case EM_PREPARACAO -> EnumSet.of(StatusPedido.PRONTO, StatusPedido.CANCELADO);
            case PRONTO -> tipoEntrega == TipoEntrega.ENTREGA
                    ? EnumSet.of(
                            StatusPedido.SAIU_PARA_ENTREGA,
                            StatusPedido.CANCELADO
                    )
                    : EnumSet.of(StatusPedido.ENTREGUE, StatusPedido.CANCELADO);
            case SAIU_PARA_ENTREGA -> EnumSet.of(StatusPedido.ENTREGUE, StatusPedido.CANCELADO);
            case ENTREGUE, CANCELADO -> Set.of();
        };
    }

    private void atualizarItensERecalcular(
            Pedido pedido,
            PedidoRequest request,
            List<Long> itemIds
    ) {
        if (itemIds == null || itemIds.size() != request.itens().size()) {
            throw new BusinessException("Os itens informados para edição são inválidos.");
        }

        Map<Long, ItemPedido> itensOriginais = new HashMap<>();
        pedido.getItens().forEach(item -> itensOriginais.put(item.getId(), item));

        Set<Long> idsMantidos = new HashSet<>();
        Set<Long> produtosInformados = new HashSet<>();
        BigDecimal subtotalPedido = BigDecimal.ZERO;

        for (int indice = 0; indice < request.itens().size(); indice++) {
            ItemPedidoRequest itemRequest = request.itens().get(indice);
            Long itemId = itemIds.get(indice);

            if (!produtosInformados.add(itemRequest.produtoId())) {
                throw new BusinessException("Um produto não pode ser repetido no pedido.");
            }

            ItemPedido itemPedido;

            if (itemId != null) {
                itemPedido = itensOriginais.get(itemId);

                if (itemPedido == null || !idsMantidos.add(itemId)) {
                    throw new BusinessException(
                            "O item informado não pertence ao pedido em edição."
                    );
                }

                if (!itemPedido.getProduto().getId().equals(itemRequest.produtoId())) {
                    throw new BusinessException("O produto do item existente é inválido.");
                }

                atualizarItemExistente(itemPedido, itemRequest);
            } else {
                Produto produto = buscarProdutoPorId(itemRequest.produtoId());
                validarProdutoDisponivelParaVenda(produto);
                validarQuantidade(produto, itemRequest.quantidade());
                itemPedido = criarItemPedido(pedido, produto, itemRequest);
                pedido.getItens().add(itemPedido);
            }

            subtotalPedido = subtotalPedido.add(itemPedido.getSubtotal());
        }

        pedido.getItens().removeIf(item ->
                item.getId() != null && !idsMantidos.contains(item.getId())
        );
        pedido.setSubtotal(subtotalPedido);
        pedido.calcularValorTotal();
    }

    private void atualizarItemExistente(
            ItemPedido itemPedido,
            ItemPedidoRequest request
    ) {
        Produto produto = itemPedido.getProduto();
        validarQuantidade(produto, request.quantidade());

        itemPedido.setQuantidade(request.quantidade());
        itemPedido.setObservacao(Boolean.TRUE.equals(produto.getPermiteAcompanhamento())
                ? request.observacao()
                : null);
        itemPedido.setSubtotal(
                itemPedido.getPrecoUnitario()
                        .multiply(request.quantidade())
                        .setScale(2, RoundingMode.HALF_UP)
        );
    }

    private void substituirItensERecalcular(
            Pedido pedido,
            PedidoRequest request
    ) {
        pedido.getItens().clear();

        BigDecimal subtotalPedido = BigDecimal.ZERO;

        Set<Long> produtosInformados = new HashSet<>();
        for (ItemPedidoRequest itemRequest : request.itens()) {
            if (!produtosInformados.add(itemRequest.produtoId())) throw new BusinessException("Um produto não pode ser repetido no pedido.");
            Produto produto = buscarProdutoPorId(itemRequest.produtoId());

            validarProdutoDisponivelParaVenda(produto);
            validarQuantidade(produto, itemRequest.quantidade());

            ItemPedido itemPedido = criarItemPedido(
                    pedido,
                    produto,
                    itemRequest
            );

            pedido.getItens().add(itemPedido);

            subtotalPedido = subtotalPedido.add(
                    itemPedido.getSubtotal()
            );
        }

        pedido.setSubtotal(subtotalPedido);
        pedido.calcularValorTotal();
    }

    private Pedido criarPedido(
            PedidoRequest request,
            Cliente cliente
    ) {
        return Pedido.builder()
                .dataPedido(LocalDateTime.now())
                .dataAgendada(request.dataAgendada())
                .status(StatusPedido.PENDENTE)
                .formaPagamento(request.formaPagamento())
                .tipoEntrega(request.tipoEntrega())
                .subtotal(BigDecimal.ZERO)
                .taxaEntrega(normalizarTaxaEntrega(request.tipoEntrega(), request.taxaEntrega()))
                .valorTotal(BigDecimal.ZERO)
                .observacao(request.observacao())
                .cliente(cliente)
                .build();
    }

    private BigDecimal normalizarTaxaEntrega(TipoEntrega tipoEntrega, BigDecimal taxaEntrega) {
        if (tipoEntrega == TipoEntrega.RETIRADA) return BigDecimal.ZERO.setScale(2);
        return taxaEntrega != null
                ? taxaEntrega.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public PreviaEstoquePedidoResponse preverEstoque(PedidoRequest request) {
        if (request == null || request.itens() == null || request.itens().isEmpty()) throw new BusinessException("Adicione ao menos um produto para consultar o Estoque.");
        Pedido pedido = criarPedido(request, null);
        substituirItensERecalcular(pedido, request);
        return estoqueService.preverPedido(pedido);
    }

    private ItemPedido criarItemPedido(
            Pedido pedido,
            Produto produto,
            ItemPedidoRequest request
    ) {
        BigDecimal subtotalItem = produto.getPreco().multiply(
                request.quantidade()
        ).setScale(2, RoundingMode.HALF_UP);

        return ItemPedido.builder()
                .quantidade(request.quantidade())
                .precoUnitario(produto.getPreco())
                .subtotal(subtotalItem)
                .observacao(Boolean.TRUE.equals(produto.getPermiteAcompanhamento())
                        ? request.observacao()
                        : null)
                .pedido(pedido)
                .produto(produto)
                .build();
    }

    private Pedido buscarEntidadePorId(Long id) {
        return pedidoRepository.buscarDetalhado(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pedido não encontrado."
                        )
                );
    }

    private Cliente buscarClientePorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cliente não encontrado."
                        )
                );
    }

    private Produto buscarProdutoPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Produto não encontrado."
                        )
                );
    }

    private void validarProdutoDisponivelParaVenda(Produto produto) {
        if (!Boolean.TRUE.equals(produto.getAtivo()) || !Boolean.TRUE.equals(produto.getVendavel())
                || (produto.getTipoProduto()!=br.com.sergio.gestaopedidos.enums.TipoProduto.PRODUTO_COMERCIAL
                && produto.getTipoProduto()!=br.com.sergio.gestaopedidos.enums.TipoProduto.PRODUTO_REVENDA)) {
            throw new BusinessException("Este produto não está disponível para venda.");
        }
    }

    private void validarQuantidade(
            Produto produto,
            BigDecimal quantidade
    ) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new BusinessException("Quantidade deve ser maior que zero.");
        }

        if (produto.getUnidadeVenda() == UnidadeVenda.UNIDADE
                && quantidade.stripTrailingZeros().scale() > 0) {
            throw new BusinessException(
                    "A quantidade do produto " + produto.getNome()
                            + " deve ser um número inteiro."
            );
        }
    }
}
