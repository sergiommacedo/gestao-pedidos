package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioProducaoIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioProducaoLinhaResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.repository.ItemPedidoRepository;
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
public class RelatorioProducaoService {

    public static final int LIMITE_REGISTROS_SAIDA = 10_000;
    private static final int TAMANHO_LOTE_SAIDA = 500;

    private final ItemPedidoRepository itemPedidoRepository;
    private final PedidoRepository pedidoRepository;

    @Transactional(readOnly = true)
    public ResultadoRelatorioProducao buscar(FiltroRelatorioProducao filtro, Pageable pageable) {
        String produto = validarENormalizar(filtro);
        RelatorioProducaoIndicadoresResponse indicadores = buscarIndicadores(filtro, produto);
        Page<RelatorioProducaoLinhaResponse> linhas = itemPedidoRepository.buscarRelatorioProducao(
                filtro.dataInicial(), filtro.dataFinal(), produto, filtro.unidadeVenda(),
                filtro.tipoEntrega(), filtro.formaPagamento(), StatusPedido.CANCELADO, pageable
        ).map(this::normalizarLinha);
        return new ResultadoRelatorioProducao(linhas, indicadores);
    }

    @Transactional(readOnly = true)
    public ResultadoCompletoRelatorioProducao buscarParaSaida(
            FiltroRelatorioProducao filtro,
            Sort ordenacao
    ) {
        String produto = validarENormalizar(filtro);
        List<RelatorioProducaoLinhaResponse> linhas = new ArrayList<>();
        int pagina = 0;
        Slice<RelatorioProducaoLinhaResponse> lote;
        do {
            lote = itemPedidoRepository.buscarLoteRelatorioProducao(
                    filtro.dataInicial(), filtro.dataFinal(), produto, filtro.unidadeVenda(),
                    filtro.tipoEntrega(), filtro.formaPagamento(), StatusPedido.CANCELADO,
                    PageRequest.of(pagina++, TAMANHO_LOTE_SAIDA, ordenacao)
            );
            lote.getContent().stream().map(this::normalizarLinha).forEach(linhas::add);
            if (linhas.size() > LIMITE_REGISTROS_SAIDA
                    || (linhas.size() == LIMITE_REGISTROS_SAIDA && lote.hasNext())) {
                throw new BusinessException(
                        "O relatório excede o limite de 10.000 produtos. Reduza o período ou aplique mais filtros."
                );
            }
        } while (lote.hasNext());

        return new ResultadoCompletoRelatorioProducao(
                List.copyOf(linhas),
                buscarIndicadores(filtro, produto)
        );
    }

    private String validarENormalizar(FiltroRelatorioProducao filtro) {
        if (filtro.dataInicial() == null || filtro.dataFinal() == null) {
            throw new BusinessException("Informe a data inicial e a data final.");
        }
        if (filtro.dataInicial().isAfter(filtro.dataFinal())) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
        return filtro.produto() == null ? "" : filtro.produto().trim();
    }

    private RelatorioProducaoIndicadoresResponse buscarIndicadores(
            FiltroRelatorioProducao filtro,
            String produto
    ) {
        ItemPedidoRepository.IndicadoresRelatorioProducao agregado =
                itemPedidoRepository.buscarIndicadoresRelatorioProducao(
                        filtro.dataInicial(), filtro.dataFinal(), produto, filtro.unidadeVenda(),
                        filtro.tipoEntrega(), filtro.formaPagamento(), StatusPedido.CANCELADO,
                        UnidadeVenda.UNIDADE, UnidadeVenda.QUILOGRAMA
                );
        if (agregado == null) {
            return RelatorioProducaoIndicadoresResponse.vazio();
        }

        BigDecimal faturamento = zero(agregado.getFaturamentoProdutos());
        BigDecimal taxas = zero(pedidoRepository.somarTaxasRelatorioProducao(
                filtro.dataInicial(), filtro.dataFinal(), produto, filtro.unidadeVenda(),
                filtro.tipoEntrega(), filtro.formaPagamento(), StatusPedido.CANCELADO
        ));
        List<ItemPedidoRepository.ProdutoLiderRelatorioProducao> lideres =
                itemPedidoRepository.buscarProdutoLiderRelatorioProducao(
                        filtro.dataInicial(), filtro.dataFinal(), produto, filtro.unidadeVenda(),
                        filtro.tipoEntrega(), filtro.formaPagamento(), StatusPedido.CANCELADO,
                        PageRequest.of(0, 1)
                );
        String liderNome = lideres.isEmpty() ? "Nenhum" : lideres.getFirst().getProdutoNome();
        BigDecimal liderValor = lideres.isEmpty()
                ? BigDecimal.ZERO
                : zero(lideres.getFirst().getFaturamentoTotal());

        return new RelatorioProducaoIndicadoresResponse(
                agregado.getProdutosDistintos() == null ? 0 : agregado.getProdutosDistintos(),
                zero(agregado.getTotalUnidades()),
                zero(agregado.getTotalQuilogramas()),
                faturamento,
                taxas,
                faturamento.add(taxas),
                liderNome,
                liderValor
        );
    }

    private RelatorioProducaoLinhaResponse normalizarLinha(RelatorioProducaoLinhaResponse linha) {
        return new RelatorioProducaoLinhaResponse(
                linha.produtoId(), linha.produtoNome(), linha.unidadeVenda(), zero(linha.quantidadeTotal()),
                linha.pedidosDistintos(), zero(linha.faturamentoTotal()),
                zero(linha.mediaPorPedido()).setScale(2, RoundingMode.HALF_UP),
                zero(linha.participacaoPercentual()).setScale(2, RoundingMode.HALF_UP),
                linha.posicao()
        );
    }

    private BigDecimal zero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    public record FiltroRelatorioProducao(
            LocalDate dataInicial,
            LocalDate dataFinal,
            String produto,
            UnidadeVenda unidadeVenda,
            TipoEntrega tipoEntrega,
            FormaPagamento formaPagamento
    ) {
    }

    public record ResultadoRelatorioProducao(
            Page<RelatorioProducaoLinhaResponse> linhas,
            RelatorioProducaoIndicadoresResponse indicadores
    ) {
    }

    public record ResultadoCompletoRelatorioProducao(
            List<RelatorioProducaoLinhaResponse> linhas,
            RelatorioProducaoIndicadoresResponse indicadores
    ) {
    }
}
