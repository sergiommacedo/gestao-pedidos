package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.dashboard.DashboardIndicadoresResponse;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

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
}
