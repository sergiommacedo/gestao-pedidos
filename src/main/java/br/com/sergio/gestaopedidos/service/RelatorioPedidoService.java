package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoLinhaResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
public class RelatorioPedidoService {

    public static final int LIMITE_REGISTROS_SAIDA = 10_000;
    private static final int TAMANHO_LOTE_SAIDA = 500;

    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public ResultadoRelatorioPedidos buscar(FiltroRelatorioPedidos filtro, Pageable pageable) {
        validarPeriodo(filtro.dataInicial(), filtro.dataFinal());
        String cliente = filtro.cliente() == null ? "" : filtro.cliente().trim();

        Page<RelatorioPedidoLinhaResponse> pedidos = pedidoRepository.buscarRelatorioPedidos(
                filtro.dataInicial(),
                filtro.dataFinal(),
                cliente,
                filtro.status(),
                filtro.tipoEntrega(),
                filtro.formaPagamento(),
                pageable
        );
        RelatorioPedidoIndicadoresResponse indicadores = buscarIndicadores(filtro, cliente);
        return new ResultadoRelatorioPedidos(pedidos, indicadores);
    }

    @Transactional(readOnly = true)
    public ResultadoCompletoRelatorioPedidos buscarParaSaida(
            FiltroRelatorioPedidos filtro,
            Sort ordenacao
    ) {
        validarPeriodo(filtro.dataInicial(), filtro.dataFinal());
        String cliente = filtro.cliente() == null ? "" : filtro.cliente().trim();
        List<RelatorioPedidoLinhaResponse> pedidos = new ArrayList<>();
        int numeroLote = 0;
        Slice<RelatorioPedidoLinhaResponse> lote;

        do {
            lote = pedidoRepository.buscarLoteRelatorioPedidos(
                    filtro.dataInicial(),
                    filtro.dataFinal(),
                    cliente,
                    filtro.status(),
                    filtro.tipoEntrega(),
                    filtro.formaPagamento(),
                    PageRequest.of(numeroLote, TAMANHO_LOTE_SAIDA, ordenacao)
            );
            pedidos.addAll(lote.getContent());

            if (pedidos.size() > LIMITE_REGISTROS_SAIDA
                    || (pedidos.size() == LIMITE_REGISTROS_SAIDA && lote.hasNext())) {
                throw new BusinessException(
                        "O relatório excede o limite de 10.000 registros. Reduza o período ou aplique mais filtros."
                );
            }
            numeroLote++;
        } while (lote.hasNext());

        return new ResultadoCompletoRelatorioPedidos(
                List.copyOf(pedidos),
                buscarIndicadores(filtro, cliente)
        );
    }

    private RelatorioPedidoIndicadoresResponse buscarIndicadores(
            FiltroRelatorioPedidos filtro,
            String cliente
    ) {
        PedidoRepository.IndicadoresRelatorioPedidos agregado =
                pedidoRepository.buscarIndicadoresRelatorioPedidos(
                        filtro.dataInicial(),
                        filtro.dataFinal(),
                        cliente,
                        filtro.status(),
                        filtro.tipoEntrega(),
                        filtro.formaPagamento(),
                        StatusPedido.CANCELADO
                );
        return montarIndicadores(agregado);
    }

    private void validarPeriodo(LocalDate dataInicial, LocalDate dataFinal) {
        if (dataInicial == null || dataFinal == null) {
            throw new BusinessException("Informe a data inicial e a data final.");
        }
        if (dataInicial.isAfter(dataFinal)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
    }

    private RelatorioPedidoIndicadoresResponse montarIndicadores(
            PedidoRepository.IndicadoresRelatorioPedidos agregado
    ) {
        if (agregado == null) {
            return RelatorioPedidoIndicadoresResponse.vazio();
        }

        long total = valorOuZero(agregado.getTotalPedidos());
        long validos = valorOuZero(agregado.getPedidosValidos());
        long cancelados = valorOuZero(agregado.getCancelados());
        BigDecimal faturamento = valorOuZero(agregado.getFaturamento());
        BigDecimal taxas = valorOuZero(agregado.getTaxasEntrega());
        BigDecimal ticketMedio = validos == 0
                ? BigDecimal.ZERO
                : faturamento.divide(BigDecimal.valueOf(validos), 2, RoundingMode.HALF_UP);

        return new RelatorioPedidoIndicadoresResponse(
                total,
                validos,
                cancelados,
                faturamento,
                taxas,
                ticketMedio
        );
    }

    private long valorOuZero(Long valor) {
        return valor == null ? 0 : valor;
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public record FiltroRelatorioPedidos(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String cliente,
            StatusPedido status,
            TipoEntrega tipoEntrega,
            FormaPagamento formaPagamento
    ) {
    }

    public record ResultadoRelatorioPedidos(
            Page<RelatorioPedidoLinhaResponse> pedidos,
            RelatorioPedidoIndicadoresResponse indicadores
    ) {
    }

    public record ResultadoCompletoRelatorioPedidos(
            List<RelatorioPedidoLinhaResponse> pedidos,
            RelatorioPedidoIndicadoresResponse indicadores
    ) {
    }
}
