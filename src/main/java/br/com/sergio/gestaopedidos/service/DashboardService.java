package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.dashboard.DashboardIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.dashboard.DashboardPedidoAtencaoResponse;
import br.com.sergio.gestaopedidos.dto.dashboard.DashboardStatusResponse;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int LIMITE_PEDIDOS_ATENCAO = 8;
    private static final Set<StatusPedido> STATUS_ATENCAO = EnumSet.of(
            StatusPedido.PENDENTE,
            StatusPedido.EM_PREPARACAO,
            StatusPedido.PRONTO,
            StatusPedido.SAIU_PARA_ENTREGA
    );
    private static final List<StatusPedido> STATUS_RESUMO = List.of(
            StatusPedido.PENDENTE,
            StatusPedido.EM_PREPARACAO,
            StatusPedido.PRONTO,
            StatusPedido.SAIU_PARA_ENTREGA,
            StatusPedido.ENTREGUE,
            StatusPedido.CANCELADO
    );

    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public DashboardIndicadoresResponse buscarIndicadores(LocalDate dataReferencia) {
        BigDecimal faturamento = pedidoRepository.somarValorTotalPorDataExcetoStatus(
                dataReferencia,
                StatusPedido.CANCELADO
        );

        return new DashboardIndicadoresResponse(
                pedidoRepository.countByDataAgendada(dataReferencia),
                pedidoRepository.countByDataAgendadaAndStatus(
                        dataReferencia,
                        StatusPedido.EM_PREPARACAO
                ),
                pedidoRepository.countByDataAgendadaAndStatus(
                        dataReferencia,
                        StatusPedido.SAIU_PARA_ENTREGA
                ),
                faturamento == null ? BigDecimal.ZERO : faturamento
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardPedidoAtencaoResponse> buscarPedidosQuePrecisamAtencao(
            LocalDate dataReferencia
    ) {
        return pedidoRepository.buscarPedidosQuePrecisamAtencao(
                dataReferencia,
                STATUS_ATENCAO,
                StatusPedido.PRONTO,
                StatusPedido.SAIU_PARA_ENTREGA,
                StatusPedido.EM_PREPARACAO,
                StatusPedido.PENDENTE,
                PageRequest.of(0, LIMITE_PEDIDOS_ATENCAO)
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardStatusResponse> buscarResumoStatus(LocalDate dataReferencia) {
        Map<StatusPedido, Long> quantidades = new EnumMap<>(StatusPedido.class);
        pedidoRepository.contarPedidosPorStatusNaData(dataReferencia)
                .forEach(contagem -> quantidades.put(
                        contagem.getStatus(),
                        contagem.getQuantidade()
                ));

        long total = quantidades.values().stream()
                .mapToLong(Long::longValue)
                .sum();

        return STATUS_RESUMO.stream()
                .map(status -> criarResumoStatus(
                        status,
                        quantidades.getOrDefault(status, 0L),
                        total
                ))
                .toList();
    }

    private DashboardStatusResponse criarResumoStatus(
            StatusPedido status,
            long quantidade,
            long total
    ) {
        BigDecimal percentual = total == 0
                ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(quantidade)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);

        return new DashboardStatusResponse(
                status,
                status.getDescricao(),
                quantidade,
                percentual
        );
    }
}
