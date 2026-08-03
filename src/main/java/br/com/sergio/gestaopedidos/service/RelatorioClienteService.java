package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioClienteIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioClienteLinhaResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioClienteService {

    public static final int LIMITE_REGISTROS_SAIDA = 10_000;
    private static final int TAMANHO_LOTE_SAIDA = 500;

    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public ResultadoRelatorioClientes buscar(FiltroRelatorioClientes filtro, Pageable pageable) {
        String cliente = validarENormalizar(filtro);
        Slice<RelatorioClienteLinhaResponse> linhas = buscarLote(filtro, cliente, pageable)
                .map(this::normalizarLinha);
        return new ResultadoRelatorioClientes(linhas, buscarIndicadores(filtro, cliente));
    }

    @Transactional(readOnly = true)
    public ResultadoCompletoRelatorioClientes buscarParaSaida(
            FiltroRelatorioClientes filtro,
            Sort ordenacao
    ) {
        String cliente = validarENormalizar(filtro);
        List<RelatorioClienteLinhaResponse> linhas = new ArrayList<>();
        int pagina = 0;
        Slice<RelatorioClienteLinhaResponse> lote;
        do {
            lote = pedidoRepository.buscarLoteRelatorioClientes(
                    filtro.dataInicial(), filtro.dataFinal(), cliente, filtro.tipoEntrega(),
                    filtro.formaPagamento(), filtro.minimoPedidos(), filtro.minimoMovimentado(),
                    StatusPedido.CANCELADO, TipoEntrega.ENTREGA, TipoEntrega.RETIRADA,
                    PageRequest.of(pagina++, TAMANHO_LOTE_SAIDA, ordenacao)
            );
            lote.getContent().stream().map(this::normalizarLinha).forEach(linhas::add);
            if (linhas.size() > LIMITE_REGISTROS_SAIDA
                    || (linhas.size() == LIMITE_REGISTROS_SAIDA && lote.hasNext())) {
                throw new BusinessException(
                        "O relatório excede o limite de 10.000 clientes. Reduza o período ou aplique mais filtros."
                );
            }
        } while (lote.hasNext());

        return new ResultadoCompletoRelatorioClientes(
                List.copyOf(linhas), buscarIndicadores(filtro, cliente)
        );
    }

    private Slice<RelatorioClienteLinhaResponse> buscarLote(
            FiltroRelatorioClientes filtro,
            String cliente,
            Pageable pageable
    ) {
        return pedidoRepository.buscarRelatorioClientes(
                filtro.dataInicial(), filtro.dataFinal(), cliente, filtro.tipoEntrega(),
                filtro.formaPagamento(), filtro.minimoPedidos(), filtro.minimoMovimentado(),
                StatusPedido.CANCELADO, TipoEntrega.ENTREGA, TipoEntrega.RETIRADA, pageable
        );
    }

    private String validarENormalizar(FiltroRelatorioClientes filtro) {
        if (filtro.dataInicial() == null || filtro.dataFinal() == null) {
            throw new BusinessException("Informe a data inicial e a data final.");
        }
        if (filtro.dataInicial().isAfter(filtro.dataFinal())) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
        if (filtro.minimoPedidos() != null && filtro.minimoPedidos() < 1) {
            throw new BusinessException("A quantidade mínima de pedidos deve ser igual ou superior a 1.");
        }
        if (filtro.minimoMovimentado() != null
                && filtro.minimoMovimentado().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O valor mínimo movimentado não pode ser negativo.");
        }
        return filtro.cliente() == null ? "" : filtro.cliente().trim();
    }

    private RelatorioClienteIndicadoresResponse buscarIndicadores(
            FiltroRelatorioClientes filtro,
            String cliente
    ) {
        PedidoRepository.IndicadoresRelatorioClientes agregado =
                pedidoRepository.buscarIndicadoresRelatorioClientes(
                        filtro.dataInicial(), filtro.dataFinal(), cliente,
                        nome(filtro.tipoEntrega()), nome(filtro.formaPagamento()),
                        filtro.minimoPedidos(), filtro.minimoMovimentado(), StatusPedido.CANCELADO.name()
                );
        if (agregado == null) return RelatorioClienteIndicadoresResponse.vazio();

        long pedidos = valor(agregado.getPedidosValidos());
        BigDecimal faturamento = zero(agregado.getFaturamentoTotal());
        BigDecimal ticket = pedidos == 0
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);
        List<PedidoRepository.ClienteLiderRelatorio> lideres =
                pedidoRepository.buscarClienteLiderRelatorio(
                        filtro.dataInicial(), filtro.dataFinal(), cliente, filtro.tipoEntrega(),
                        filtro.formaPagamento(), filtro.minimoPedidos(), filtro.minimoMovimentado(),
                        StatusPedido.CANCELADO, PageRequest.of(0, 1)
                );

        return new RelatorioClienteIndicadoresResponse(
                valor(agregado.getClientesCompradores()), pedidos, faturamento, ticket,
                valor(agregado.getClientesRecorrentes()),
                lideres.isEmpty() ? "Nenhum" : lideres.getFirst().getClienteNome(),
                lideres.isEmpty() ? BigDecimal.ZERO : zero(lideres.getFirst().getFaturamentoTotal())
        );
    }

    private RelatorioClienteLinhaResponse normalizarLinha(RelatorioClienteLinhaResponse linha) {
        long pedidos = valor(linha.pedidosValidos());
        BigDecimal faturamento = zero(linha.faturamentoTotal());
        BigDecimal ticket = pedidos == 0
                ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);
        return new RelatorioClienteLinhaResponse(
                linha.clienteId(), linha.clienteNome(), linha.clienteTelefone(), pedidos,
                faturamento, ticket, linha.primeiraCompra(), linha.ultimaCompra(),
                valor(linha.entregas()), valor(linha.retiradas()),
                zero(linha.participacaoPercentual()).setScale(2, RoundingMode.HALF_UP), linha.posicao()
        );
    }

    private String nome(Enum<?> valor) { return valor == null ? null : valor.name(); }
    private long valor(Long valor) { return valor == null ? 0 : valor; }
    private BigDecimal zero(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }

    public record FiltroRelatorioClientes(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String cliente,
            TipoEntrega tipoEntrega,
            FormaPagamento formaPagamento,
            Long minimoPedidos,
            BigDecimal minimoMovimentado
    ) {
    }

    public record ResultadoRelatorioClientes(
            Slice<RelatorioClienteLinhaResponse> linhas,
            RelatorioClienteIndicadoresResponse indicadores
    ) {
    }

    public record ResultadoCompletoRelatorioClientes(
            List<RelatorioClienteLinhaResponse> linhas,
            RelatorioClienteIndicadoresResponse indicadores
    ) {
    }
}
