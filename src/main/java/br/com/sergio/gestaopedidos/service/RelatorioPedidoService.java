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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RelatorioPedidoService {

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

        RelatorioPedidoIndicadoresResponse indicadores = montarIndicadores(agregado);
        return new ResultadoRelatorioPedidos(pedidos, indicadores);
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
}
