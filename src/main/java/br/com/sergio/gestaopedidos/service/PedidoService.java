package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.pedido.ItemPedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoRequest;
import br.com.sergio.gestaopedidos.dto.pedido.PedidoResponse;
import br.com.sergio.gestaopedidos.entity.Cliente;
import br.com.sergio.gestaopedidos.entity.ItemPedido;
import br.com.sergio.gestaopedidos.entity.Pedido;
import br.com.sergio.gestaopedidos.entity.Produto;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import br.com.sergio.gestaopedidos.mapper.PedidoMapper;
import br.com.sergio.gestaopedidos.repository.ClienteRepository;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import br.com.sergio.gestaopedidos.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoMapper pedidoMapper;

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
            Pageable pageable
    ) {
        String filtroTratado = filtro == null ? "" : filtro.trim();

        return pedidoRepository.buscarPaginado(
                        filtroTratado,
                        status,
                        dataAgendada,
                        pageable
                )
                .map(pedidoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public long contarPedidosDeHoje() {
        return pedidoRepository.countByDataAgendadaAndStatusNot(
                LocalDate.now(),
                StatusPedido.CANCELADO
        );
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        Pedido pedido = buscarEntidadePorId(id);
        return pedidoMapper.toResponse(pedido);
    }

    public PedidoResponse salvar(PedidoRequest request) {
        Cliente cliente = buscarClientePorId(request.clienteId());

        Pedido pedido = criarPedido(request, cliente);

        BigDecimal subtotalPedido = BigDecimal.ZERO;

        for (ItemPedidoRequest itemRequest : request.itens()) {
            Produto produto = buscarProdutoPorId(itemRequest.produtoId());

            validarProdutoAtivo(produto);

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

        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        return pedidoMapper.toResponse(pedidoSalvo);
    }

    private Pedido criarPedido(
            PedidoRequest request,
            Cliente cliente
    ) {
        BigDecimal taxaEntrega = request.taxaEntrega() != null
                ? request.taxaEntrega()
                : BigDecimal.ZERO;

        return Pedido.builder()
                .dataPedido(LocalDateTime.now())
                .dataAgendada(request.dataAgendada())
                .status(StatusPedido.PENDENTE)
                .formaPagamento(request.formaPagamento())
                .tipoEntrega(request.tipoEntrega())
                .subtotal(BigDecimal.ZERO)
                .taxaEntrega(taxaEntrega)
                .valorTotal(BigDecimal.ZERO)
                .observacao(request.observacao())
                .cliente(cliente)
                .build();
    }

    private ItemPedido criarItemPedido(
            Pedido pedido,
            Produto produto,
            ItemPedidoRequest request
    ) {
        BigDecimal subtotalItem = produto.getPreco().multiply(
                BigDecimal.valueOf(request.quantidade())
        );

        return ItemPedido.builder()
                .quantidade(request.quantidade())
                .precoUnitario(produto.getPreco())
                .subtotal(subtotalItem)
                .pedido(pedido)
                .produto(produto)
                .build();
    }

    private Pedido buscarEntidadePorId(Long id) {
        return pedidoRepository.findById(id)
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

    private void validarProdutoAtivo(Produto produto) {
        if (!Boolean.TRUE.equals(produto.getAtivo())) {
            throw new BusinessException(
                    "O produto " + produto.getNome()
                            + " está inativo e não pode ser adicionado ao pedido."
            );
        }
    }
}
