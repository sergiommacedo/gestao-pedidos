package br.com.sergio.gestaopedidos.dto.compra;

import br.com.sergio.gestaopedidos.enums.TipoItemEstoque;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AnalisePrecosCompraResponse(List<FornecedorItem> fornecedores,
                                          List<ComparativoItem> comparativos) {
    public record FornecedorItem(TipoItemEstoque categoria, Long referenciaId, String item,
                                 String fornecedor, UnidadeMedida unidade, LocalDate ultimaCompra,
                                 BigDecimal ultimoPreco, BigDecimal menorPreco, BigDecimal maiorPreco,
                                 BigDecimal precoMedioPonderado, BigDecimal quantidadeTotal) {}
    public record PrecoFornecedor(String fornecedor, BigDecimal preco) {}
    public record ComparativoItem(TipoItemEstoque categoria, Long referenciaId, String item,
                                  UnidadeMedida unidade, List<PrecoFornecedor> melhores,
                                  List<PrecoFornecedor> piores, BigDecimal diferenca,
                                  int quantidadeFornecedores) {
        public boolean empateMelhor() { return melhores.size() > 1; }
        public boolean empatePior() { return piores.size() > 1; }
        public boolean fornecedorUnico() { return quantidadeFornecedores == 1; }
    }
    public record HistoricoItem(LocalDate data, String fornecedor, BigDecimal quantidade,
                                BigDecimal valorPago, BigDecimal precoUnitario) {}
}
