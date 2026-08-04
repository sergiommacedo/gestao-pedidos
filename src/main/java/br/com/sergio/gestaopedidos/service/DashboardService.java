package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.dashboard.*;
import br.com.sergio.gestaopedidos.dto.resumo.ResumoProdutoVendidoResponse;
import br.com.sergio.gestaopedidos.dto.resumo.ResumoVendasDiaResponse;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.repository.*;
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

    private static final int LIMITE_PEDIDOS_ATENCAO = 10;
    private static final int LIMITE_LISTAS = 5;
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
    private final ItemPedidoRepository itemPedidoRepository;
    private final ProducaoRepository producaoRepository;
    private final SaldoEstoqueRepository saldoEstoqueRepository;
    private final CompraRepository compraRepository;
    private final FichaTecnicaRepository fichaTecnicaRepository;
    private final EstoqueService estoqueService;
    private final ProducaoService producaoService;

    @Transactional(readOnly = true)
    public DashboardOperacionalResponse buscarDashboard(LocalDate data) {
        DashboardIndicadoresResponse pedidos = buscarIndicadores(data);
        var producao = montarProducao(producaoRepository.resumirConfirmadasDashboard(data));
        var rascunhos = montarRascunhos(data);
        var estoque = montarEstoque();
        var compras = montarCompras(compraRepository.resumirDashboard(data));
        long fichasPendentes = fichaTecnicaRepository.contarAtivasComCustoPendente();
        long produtosSemFicha = fichaTecnicaRepository.contarProdutosProduzidosAtivosSemFicha();
        return new DashboardOperacionalResponse(data, pedidos, buscarPedidosQuePrecisamAtencao(data),
                buscarResumoStatus(data), producao, rascunhos, estoque, compras,
                itemPedidoRepository.resumirProdutosMaisVendidosDashboard(data, StatusPedido.CANCELADO,
                        PageRequest.of(0, LIMITE_LISTAS)),
                montarAlertas(rascunhos, estoque, fichasPendentes, produtosSemFicha));
    }

    @Transactional(readOnly = true)
    public DashboardIndicadoresResponse buscarIndicadores(LocalDate dataReferencia) {
        PedidoRepository.ResumoDashboard r = pedidoRepository.resumirDashboard(dataReferencia,
                StatusPedido.CANCELADO, StatusPedido.EM_PREPARACAO, StatusPedido.SAIU_PARA_ENTREGA);
        return new DashboardIndicadoresResponse(numero(r.getTotal()), numero(r.getValidos()), numero(r.getCancelados()),
                numero(r.getEmPreparacao()), numero(r.getSaiuParaEntrega()), valorOuZero(r.getProdutos()),
                valorOuZero(r.getTaxas()), valorOuZero(r.getFaturamento()));
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

    @Transactional(readOnly = true)
    public ResumoVendasDiaResponse buscarResumoVendasDia(LocalDate dataReferencia) {
        List<ResumoProdutoVendidoResponse> produtos =
                itemPedidoRepository.resumirProdutosVendidosPorDataExcetoStatus(
                        dataReferencia,
                        StatusPedido.CANCELADO
                );

        BigDecimal totalProdutos = produtos.stream()
                .map(ResumoProdutoVendidoResponse::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalTaxasEntrega = valorOuZero(
                pedidoRepository.somarTaxasEntregaPorDataExcetoStatus(
                        dataReferencia,
                        StatusPedido.CANCELADO
                )
        );

        return new ResumoVendasDiaResponse(
                dataReferencia,
                List.copyOf(produtos),
                totalProdutos,
                totalTaxasEntrega,
                totalProdutos.add(totalTaxasEntrega)
        );
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private long numero(Long valor) { return valor == null ? 0 : valor; }

    private DashboardOperacionalResponse.ProducaoDia montarProducao(ProducaoRepository.ResumoDashboard r) {
        return r == null
                ? new DashboardOperacionalResponse.ProducaoDia(0, 0, BigDecimal.ZERO, BigDecimal.ZERO)
                : new DashboardOperacionalResponse.ProducaoDia(numero(r.getProducoes()), (int) numero(r.getProdutos()),
                    valorOuZero(r.getQuantidade()), valorOuZero(r.getCusto()));
    }

    private List<DashboardOperacionalResponse.ProducaoRascunho> montarRascunhos(LocalDate data) {
        return producaoRepository.buscarRascunhosDashboard(data).stream().map(p -> {
            var detalhes = producaoService.buscarDetalhes(p.getId());
            var preparacoes = detalhes.estoquesPreparacoes().stream()
                    .map(e -> new DashboardOperacionalResponse.PreparacaoRascunho(e.produtoNome(), e.unidade(),
                            e.producaoAdicionada(), e.estoqueAntes()))
                    .toList();
            return new DashboardOperacionalResponse.ProducaoRascunho(p.getId(), preparacoes,
                    detalhes.resumo().producao().custoTotal());
        }).toList();
    }

    private DashboardOperacionalResponse.EstoqueResumo montarEstoque() {
        SaldoEstoqueRepository.ResumoDashboard r = saldoEstoqueRepository.resumirDashboard();
        var valores = estoqueService.indicadores();
        List<DashboardOperacionalResponse.ItemEstoque> alertas = saldoEstoqueRepository
                .listarAlertasDashboard(PageRequest.of(0, LIMITE_LISTAS)).stream().map(this::mapearEstoque).toList();
        List<DashboardOperacionalResponse.ItemEstoque> produzidos = saldoEstoqueRepository
                .listarProduzidosDisponiveisDashboard(PageRequest.of(0, LIMITE_LISTAS)).stream().map(this::mapearEstoque).toList();
        return new DashboardOperacionalResponse.EstoqueResumo(numero(r.getItensComSaldo()), numero(r.getAbaixoDoMinimo()),
                numero(r.getSemEstoque()), numero(r.getProduzidosDisponiveis()), numero(r.getRevendaDisponiveis()),
                valores.valorInsumos(), valores.valorRevenda(), valores.valorProduzidos(),
                valores.valorTotal(), alertas, produzidos);
    }

    private DashboardOperacionalResponse.ItemEstoque mapearEstoque(SaldoEstoqueRepository.Visao v) {
        BigDecimal saldo = valorOuZero(v.getQuantidadeAtual()), minimo = valorOuZero(v.getEstoqueMinimo());
        String situacao = saldo.signum() == 0 ? "Sem estoque" : minimo.signum() > 0 && saldo.compareTo(minimo) <= 0
                ? "Estoque baixo" : "Normal";
        return new DashboardOperacionalResponse.ItemEstoque(br.com.sergio.gestaopedidos.enums.TipoItemEstoque.valueOf(v.getTipoItem()),
                v.getReferenciaId(), v.getItemNome(), br.com.sergio.gestaopedidos.enums.UnidadeMedida.valueOf(v.getUnidade()),
                saldo, minimo, situacao);
    }

    private DashboardOperacionalResponse.ComprasDia montarCompras(CompraRepository.ResumoDashboard r) {
        return new DashboardOperacionalResponse.ComprasDia(numero(r.getQuantidade()), valorOuZero(r.getValorTotal()),
                numero(r.getComprasInsumos()), valorOuZero(r.getValorInsumos()), numero(r.getComprasRevenda()),
                valorOuZero(r.getValorRevenda()));
    }

    private List<DashboardOperacionalResponse.Alerta> montarAlertas(List<DashboardOperacionalResponse.ProducaoRascunho> rascunhos,
            DashboardOperacionalResponse.EstoqueResumo estoque, long fichasPendentes, long produtosSemFicha) {
        List<DashboardOperacionalResponse.Alerta> alertas = new java.util.ArrayList<>();
        if (estoque.semEstoque() > 0) alertas.add(new DashboardOperacionalResponse.Alerta("bi-x-circle", "danger", "Itens sem estoque", estoque.semEstoque()+" item(ns) estão sem estoque.", "/estoque?situacao=SEM_ESTOQUE", true));
        if (estoque.abaixoDoMinimo() > 0) alertas.add(new DashboardOperacionalResponse.Alerta("bi-exclamation-triangle", "warning", "Estoque abaixo do mínimo", estoque.abaixoDoMinimo()+" item(ns) precisam de atenção.", "/estoque?situacao=BAIXO", true));
        if (fichasPendentes > 0) alertas.add(new DashboardOperacionalResponse.Alerta("bi-calculator", "warning", "Custo de ficha pendente", fichasPendentes+" Ficha(s) Técnica(s) possuem custo incompleto.", "/fichas-tecnicas?custo=PENDENTE", true));
        if (produtosSemFicha > 0) alertas.add(new DashboardOperacionalResponse.Alerta("bi-journal-x", "warning", "Produtos sem Ficha Técnica", produtosSemFicha+" Produto(s) Produzido(s) ativo(s) ainda não possuem ficha.", "/fichas-tecnicas/nova", true));
        if (!rascunhos.isEmpty()) alertas.add(new DashboardOperacionalResponse.Alerta("bi-hourglass-split", "info", "Produção em rascunho", rascunhos.size()+" Produção(ões) aguardam confirmação.", "/producoes", true));
        return List.copyOf(alertas);
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
