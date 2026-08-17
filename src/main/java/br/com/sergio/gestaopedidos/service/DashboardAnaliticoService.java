package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.dashboard.DashboardAnaliticoResponse;
import br.com.sergio.gestaopedidos.dto.dashboard.HistoricoClientePedidosResponse;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardAnaliticoService {
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public DashboardAnaliticoResponse buscar(LocalDate data) {
        LocalDate inicio = data.minusDays(6);
        return new DashboardAnaliticoResponse(
                numero("SELECT COUNT(DISTINCT ip.produto_id) FROM itens_pedido ip JOIN pedidos p ON p.id=ip.pedido_id WHERE p.data_agendada=? AND p.status<>'CANCELADO'", data),
                numero("SELECT COUNT(DISTINCT ip.produto_id) FROM itens_pedido ip JOIN pedidos p ON p.id=ip.pedido_id JOIN produtos pr ON pr.id=ip.produto_id WHERE p.data_agendada=? AND p.status<>'CANCELADO' AND pr.tipo_produto='PRODUTO_REVENDA'", data),
                numero("SELECT COUNT(*) FROM pedidos WHERE data_agendada<? AND status NOT IN ('ENTREGUE','CANCELADO')", data),
                numero("SELECT COUNT(*) FROM pedidos WHERE data_agendada=? AND status='ENTREGUE'", data),
                null,
                jdbc.query("SELECT data_agendada,COALESCE(SUM(valor_total),0) FROM pedidos WHERE data_agendada BETWEEN ? AND ? AND status<>'CANCELADO' GROUP BY data_agendada ORDER BY data_agendada", (rs,n)->new DashboardAnaliticoResponse.SerieDia(rs.getObject(1,LocalDate.class),rs.getBigDecimal(2)),inicio,data),
                jdbc.query("SELECT data_producao,COALESCE(SUM(i.quantidade),0) FROM producoes p JOIN itens_producao i ON i.producao_id=p.id WHERE p.data_producao BETWEEN ? AND ? AND p.status='CONFIRMADA' GROUP BY data_producao ORDER BY data_producao", (rs,n)->new DashboardAnaliticoResponse.SerieDia(rs.getObject(1,LocalDate.class),rs.getBigDecimal(2)),inicio,data),
                buscarRankingClientes(),
                jdbc.query("SELECT m.nome_historico,COALESCE(SUM(m.quantidade),0) total FROM movimentacoes_estoque m WHERE m.tipo='SAIDA_CONSUMO_PRODUCAO' AND m.data_movimentacao>=? AND m.data_movimentacao<? GROUP BY m.insumo_id,m.nome_historico ORDER BY total DESC LIMIT 5", (rs,n)->new DashboardAnaliticoResponse.Ranking(rs.getString(1),rs.getBigDecimal(2)),inicio.atStartOfDay(),data.plusDays(1).atStartOfDay())
        );
    }

    List<DashboardAnaliticoResponse.RankingCliente> buscarRankingClientes() {
        return jdbc.query("SELECT c.id,c.nome,COUNT(*) quantidade_pedidos,COALESCE(SUM(p.valor_total),0) valor_total,COALESCE(SUM(p.valor_total)/NULLIF(COUNT(*),0),0) ticket_medio FROM pedidos p JOIN clientes c ON c.id=p.cliente_id WHERE p.status<>'CANCELADO' GROUP BY c.id,c.nome ORDER BY valor_total DESC,quantidade_pedidos DESC,c.nome ASC LIMIT 5",
                (rs, n) -> new DashboardAnaliticoResponse.RankingCliente(rs.getLong(1), rs.getString(2),
                        rs.getLong(3), rs.getBigDecimal(4), rs.getBigDecimal(5)));
    }

    @Transactional(readOnly = true)
    public HistoricoClientePedidosResponse buscarHistoricoCliente(Long clienteId, int pagina, int tamanho,
                                                                   HistoricoClientePedidosResponse.Periodo periodo,
                                                                   LocalDate dataReferencia) {
        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), 20);
        var periodoSeguro = periodo == null ? HistoricoClientePedidosResponse.Periodo.TODO_HISTORICO : periodo;
        LocalDate referenciaSegura = dataReferencia == null ? LocalDate.now() : dataReferencia;
        LocalDate inicio = periodoSeguro == HistoricoClientePedidosResponse.Periodo.ULTIMOS_7_DIAS
                ? referenciaSegura.minusDays(6) : null;
        String filtroPeriodo = periodoSeguro == HistoricoClientePedidosResponse.Periodo.ULTIMOS_7_DIAS
                ? " AND p.data_agendada BETWEEN ? AND ?" : "";
        Object[] argumentosResumo = inicio == null
                ? new Object[]{clienteId} : new Object[]{inicio, referenciaSegura, clienteId};
        var resumos = jdbc.query("SELECT c.nome,COUNT(p.id),COALESCE(SUM(p.valor_total),0),COALESCE(SUM(p.valor_total)/NULLIF(COUNT(p.id),0),0) FROM clientes c LEFT JOIN pedidos p ON p.cliente_id=c.id AND p.status<>'CANCELADO'" + filtroPeriodo + " WHERE c.id=? GROUP BY c.id,c.nome",
                (rs, n) -> new Object[]{rs.getString(1), rs.getLong(2), rs.getBigDecimal(3), rs.getBigDecimal(4)}, argumentosResumo);
        if (resumos.isEmpty()) throw new ResourceNotFoundException("Cliente não encontrado.");
        Object[] resumo = resumos.getFirst();
        long quantidade = (Long) resumo[1];
        int totalPaginas = quantidade == 0 ? 0 : (int) Math.ceil((double) quantidade / tamanhoSeguro);
        if (totalPaginas > 0) paginaSegura = Math.min(paginaSegura, totalPaginas - 1);
        String sqlPedidos = "SELECT p.id,p.data_agendada,p.horario_inicio,p.tipo_entrega,p.status,p.subtotal,COALESCE(p.percentual_desconto_geral,0),COALESCE(p.valor_desconto_geral,0),p.taxa_entrega,p.valor_total FROM pedidos p WHERE p.cliente_id=? AND p.status<>'CANCELADO'" +
                (inicio == null ? "" : " AND p.data_agendada BETWEEN ? AND ?") +
                " ORDER BY p.data_agendada DESC,p.data_pedido DESC,p.id DESC LIMIT ? OFFSET ?";
        Object[] argumentosPedidos = inicio == null
                ? new Object[]{clienteId, tamanhoSeguro, paginaSegura * tamanhoSeguro}
                : new Object[]{clienteId, inicio, referenciaSegura, tamanhoSeguro, paginaSegura * tamanhoSeguro};
        List<HistoricoClientePedidosResponse.Pedido> pedidos = jdbc.query(sqlPedidos,
                (rs, n) -> new HistoricoClientePedidosResponse.Pedido(rs.getLong(1),
                        rs.getObject(2, LocalDate.class), rs.getObject(3, java.time.LocalTime.class),
                        TipoEntrega.valueOf(rs.getString(4)), StatusPedido.valueOf(rs.getString(5)),
                        rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8),
                        rs.getBigDecimal(9), rs.getBigDecimal(10)), argumentosPedidos);
        return new HistoricoClientePedidosResponse(clienteId, (String) resumo[0], quantidade,
                (BigDecimal) resumo[2], (BigDecimal) resumo[3], pedidos, paginaSegura, totalPaginas,
                periodoSeguro, inicio, referenciaSegura);
    }

    @Transactional(readOnly = true)
    public HistoricoClientePedidosResponse.DetalhesItens buscarItensHistoricos(Long clienteId, Long pedidoId) {
        var totais = jdbc.query("SELECT subtotal,COALESCE(percentual_desconto_geral,0),COALESCE(valor_desconto_geral,0),taxa_entrega,valor_total FROM pedidos WHERE id=? AND cliente_id=? AND status<>'CANCELADO'",
                (rs, n) -> new BigDecimal[]{rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5)}, pedidoId, clienteId);
        if (totais.isEmpty()) throw new ResourceNotFoundException("Pedido não encontrado para este cliente.");
        List<HistoricoClientePedidosResponse.Item> itens = jdbc.query("SELECT COALESCE(ip.nome_historico,pr.nome),ip.quantidade,COALESCE(ip.unidade_historica,pr.unidade_venda),COALESCE(ip.preco_unitario_original,ip.preco_unitario),COALESCE(ip.percentual_desconto,0),ip.preco_unitario,COALESCE(ip.preco_unitario_original,ip.preco_unitario)-ip.preco_unitario,ip.subtotal FROM itens_pedido ip JOIN produtos pr ON pr.id=ip.produto_id WHERE ip.pedido_id=? ORDER BY ip.id",
                (rs, n) -> new HistoricoClientePedidosResponse.Item(rs.getString(1), rs.getBigDecimal(2),
                        UnidadeVenda.valueOf(rs.getString(3)), rs.getBigDecimal(4), rs.getBigDecimal(5),
                        rs.getBigDecimal(6), rs.getBigDecimal(7), rs.getBigDecimal(8)), pedidoId);
        BigDecimal[] total = totais.getFirst();
        return new HistoricoClientePedidosResponse.DetalhesItens(itens, total[0], total[1], total[2], total[3], total[4]);
    }

    private long numero(String sql,Object... args){Long valor=jdbc.queryForObject(sql,Long.class,args);return valor==null?0:valor;}
}
