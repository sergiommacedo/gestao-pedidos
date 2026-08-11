package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.dashboard.DashboardAnaliticoResponse;
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
                buscarRankingClientes(inicio, data),
                jdbc.query("SELECT m.nome_historico,COALESCE(SUM(m.quantidade),0) total FROM movimentacoes_estoque m WHERE m.tipo='SAIDA_CONSUMO_PRODUCAO' AND m.data_movimentacao>=? AND m.data_movimentacao<? GROUP BY m.insumo_id,m.nome_historico ORDER BY total DESC LIMIT 5", (rs,n)->new DashboardAnaliticoResponse.Ranking(rs.getString(1),rs.getBigDecimal(2)),inicio.atStartOfDay(),data.plusDays(1).atStartOfDay())
        );
    }

    List<DashboardAnaliticoResponse.RankingCliente> buscarRankingClientes(LocalDate inicio, LocalDate fim) {
        return jdbc.query("SELECT c.nome,COUNT(*) quantidade_pedidos,COALESCE(SUM(p.valor_total),0) valor_total,COALESCE(SUM(p.valor_total)/NULLIF(COUNT(*),0),0) ticket_medio FROM pedidos p JOIN clientes c ON c.id=p.cliente_id WHERE p.data_agendada BETWEEN ? AND ? AND p.status<>'CANCELADO' GROUP BY c.id,c.nome ORDER BY valor_total DESC LIMIT 5",
                (rs, n) -> new DashboardAnaliticoResponse.RankingCliente(rs.getString(1), rs.getLong(2),
                        rs.getBigDecimal(3), rs.getBigDecimal(4)), inicio, fim);
    }

    private long numero(String sql,Object... args){Long valor=jdbc.queryForObject(sql,Long.class,args);return valor==null?0:valor;}
}
