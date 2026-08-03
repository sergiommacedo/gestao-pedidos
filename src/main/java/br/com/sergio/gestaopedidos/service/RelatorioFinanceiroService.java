package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.*;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioFinanceiroService {

    public static final int LIMITE_DIAS_SAIDA = 10_000;
    private static final int TAMANHO_LOTE_SAIDA = 500;

    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public ResultadoRelatorioFinanceiro buscar(FiltroRelatorioFinanceiro filtro, Pageable pageable) {
        String cliente = validarENormalizar(filtro);
        Page<RelatorioFinanceiroDiaResponse> dias = pedidoRepository.buscarDiasRelatorioFinanceiro(
                filtro.dataInicial(), filtro.dataFinal(), filtro.formaPagamento(), filtro.tipoEntrega(),
                filtro.status(), cliente, StatusPedido.CANCELADO, pageable
        ).map(this::normalizarDia);
        Agregados agregados = buscarAgregados(filtro, cliente);
        return new ResultadoRelatorioFinanceiro(
                dias, agregados.indicadores(), agregados.pagamentos(), agregados.entregas()
        );
    }

    @Transactional(readOnly = true)
    public ResultadoCompletoRelatorioFinanceiro buscarParaSaida(
            FiltroRelatorioFinanceiro filtro,
            Sort ordenacao
    ) {
        String cliente = validarENormalizar(filtro);
        List<RelatorioFinanceiroDiaResponse> dias = new ArrayList<>();
        int pagina = 0;
        Slice<RelatorioFinanceiroDiaResponse> lote;
        do {
            lote = pedidoRepository.buscarLoteDiasRelatorioFinanceiro(
                    filtro.dataInicial(), filtro.dataFinal(), filtro.formaPagamento(), filtro.tipoEntrega(),
                    filtro.status(), cliente, StatusPedido.CANCELADO,
                    PageRequest.of(pagina++, TAMANHO_LOTE_SAIDA, ordenacao)
            );
            lote.getContent().stream().map(this::normalizarDia).forEach(dias::add);
            if (dias.size() > LIMITE_DIAS_SAIDA
                    || (dias.size() == LIMITE_DIAS_SAIDA && lote.hasNext())) {
                throw new BusinessException(
                        "O relatório excede o limite de 10.000 dias. Reduza o período."
                );
            }
        } while (lote.hasNext());

        Agregados agregados = buscarAgregados(filtro, cliente);
        return new ResultadoCompletoRelatorioFinanceiro(
                List.copyOf(dias), agregados.indicadores(), agregados.pagamentos(), agregados.entregas()
        );
    }

    private String validarENormalizar(FiltroRelatorioFinanceiro filtro) {
        if (filtro.dataInicial() == null || filtro.dataFinal() == null) {
            throw new BusinessException("Informe a data inicial e a data final.");
        }
        if (filtro.dataInicial().isAfter(filtro.dataFinal())) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
        long dias = ChronoUnit.DAYS.between(filtro.dataInicial(), filtro.dataFinal()) + 1;
        if (dias > LIMITE_DIAS_SAIDA) {
            throw new BusinessException("O período não pode exceder 10.000 dias.");
        }
        return filtro.cliente() == null ? "" : filtro.cliente().trim();
    }

    private Agregados buscarAgregados(FiltroRelatorioFinanceiro filtro, String cliente) {
        PedidoRepository.IndicadoresRelatorioFinanceiro agregado =
                pedidoRepository.buscarIndicadoresRelatorioFinanceiro(
                        filtro.dataInicial(), filtro.dataFinal(), filtro.formaPagamento(), filtro.tipoEntrega(),
                        filtro.status(), cliente, StatusPedido.CANCELADO
                );
        RelatorioFinanceiroIndicadoresResponse indicadores = montarIndicadores(filtro, cliente, agregado);
        List<RelatorioFinanceiroPagamentoResponse> pagamentos =
                pedidoRepository.buscarPagamentosRelatorioFinanceiro(
                        filtro.dataInicial(), filtro.dataFinal(), filtro.formaPagamento(), filtro.tipoEntrega(),
                        filtro.status(), cliente, StatusPedido.CANCELADO
                ).stream().map(this::normalizarPagamento).toList();
        List<RelatorioFinanceiroEntregaResponse> entregas =
                pedidoRepository.buscarEntregasRelatorioFinanceiro(
                        filtro.dataInicial(), filtro.dataFinal(), filtro.formaPagamento(), filtro.tipoEntrega(),
                        filtro.status(), cliente, StatusPedido.CANCELADO
                ).stream().map(this::normalizarEntrega).toList();
        return new Agregados(indicadores, pagamentos, entregas);
    }

    private RelatorioFinanceiroIndicadoresResponse montarIndicadores(
            FiltroRelatorioFinanceiro filtro,
            String cliente,
            PedidoRepository.IndicadoresRelatorioFinanceiro agregado
    ) {
        if (agregado == null) return RelatorioFinanceiroIndicadoresResponse.vazio();
        long pedidosValidos = valor(agregado.getPedidosValidos());
        BigDecimal faturamento = zero(agregado.getFaturamentoBruto());
        BigDecimal ticket = pedidosValidos == 0 ? BigDecimal.ZERO.setScale(2)
                : faturamento.divide(BigDecimal.valueOf(pedidosValidos), 2, RoundingMode.HALF_UP);
        List<PedidoRepository.MelhorDiaRelatorioFinanceiro> melhores =
                pedidoRepository.buscarMelhorDiaRelatorioFinanceiro(
                        filtro.dataInicial(), filtro.dataFinal(), filtro.formaPagamento(), filtro.tipoEntrega(),
                        filtro.status(), cliente, StatusPedido.CANCELADO, PageRequest.of(0, 1)
                );
        return new RelatorioFinanceiroIndicadoresResponse(
                valor(agregado.getPedidosTotais()), pedidosValidos, valor(agregado.getCancelados()),
                zero(agregado.getFaturamentoProdutos()), zero(agregado.getTaxasEntrega()), faturamento,
                ticket, zero(agregado.getValorCancelado()),
                melhores.isEmpty() ? null : melhores.getFirst().getData(),
                melhores.isEmpty() ? BigDecimal.ZERO : zero(melhores.getFirst().getFaturamento())
        );
    }

    private RelatorioFinanceiroDiaResponse normalizarDia(RelatorioFinanceiroDiaResponse dia) {
        long pedidos = valor(dia.pedidosValidos());
        BigDecimal total = zero(dia.faturamentoTotal());
        BigDecimal ticket = pedidos == 0 ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);
        return new RelatorioFinanceiroDiaResponse(
                dia.data(), pedidos, valor(dia.cancelados()), zero(dia.faturamentoProdutos()),
                zero(dia.taxasEntrega()), total, ticket
        );
    }

    private RelatorioFinanceiroPagamentoResponse normalizarPagamento(RelatorioFinanceiroPagamentoResponse item) {
        long pedidos = valor(item.pedidos());
        BigDecimal faturamento = zero(item.faturamento());
        return new RelatorioFinanceiroPagamentoResponse(
                item.formaPagamento(), pedidos, faturamento,
                zero(item.participacaoPercentual()).setScale(2, RoundingMode.HALF_UP),
                pedidos == 0 ? BigDecimal.ZERO.setScale(2)
                        : faturamento.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP)
        );
    }

    private RelatorioFinanceiroEntregaResponse normalizarEntrega(RelatorioFinanceiroEntregaResponse item) {
        return new RelatorioFinanceiroEntregaResponse(
                item.tipoEntrega(), valor(item.pedidos()), zero(item.faturamentoProdutos()),
                zero(item.taxasEntrega()), zero(item.faturamentoTotal()),
                zero(item.participacaoPercentual()).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private long valor(Long valor) { return valor == null ? 0 : valor; }
    private BigDecimal zero(BigDecimal valor) { return valor == null ? BigDecimal.ZERO : valor; }

    public record FiltroRelatorioFinanceiro(
            LocalDate dataInicial, LocalDate dataFinal, FormaPagamento formaPagamento,
            TipoEntrega tipoEntrega, StatusPedido status, String cliente
    ) {}

    public record ResultadoRelatorioFinanceiro(
            Page<RelatorioFinanceiroDiaResponse> dias,
            RelatorioFinanceiroIndicadoresResponse indicadores,
            List<RelatorioFinanceiroPagamentoResponse> pagamentos,
            List<RelatorioFinanceiroEntregaResponse> entregas
    ) {}

    public record ResultadoCompletoRelatorioFinanceiro(
            List<RelatorioFinanceiroDiaResponse> dias,
            RelatorioFinanceiroIndicadoresResponse indicadores,
            List<RelatorioFinanceiroPagamentoResponse> pagamentos,
            List<RelatorioFinanceiroEntregaResponse> entregas
    ) {}

    private record Agregados(
            RelatorioFinanceiroIndicadoresResponse indicadores,
            List<RelatorioFinanceiroPagamentoResponse> pagamentos,
            List<RelatorioFinanceiroEntregaResponse> entregas
    ) {}
}
