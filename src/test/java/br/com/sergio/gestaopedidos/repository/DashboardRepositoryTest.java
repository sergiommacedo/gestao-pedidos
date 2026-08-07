package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class DashboardRepositoryTest {
    @Autowired EntityManager em;
    @Autowired SaldoEstoqueRepository saldos;
    @Autowired CompraRepository compras;

    @Test
    void indicadoresUsamAsTresCategoriasAtivasSemSobreporSemEstoqueEBaixo() {
        Insumo zerado = insumo("Zerado", true, "5");
        Insumo negativo = insumo("Negativo", true, "5");
        Insumo abaixo = insumo("Abaixo", true, "5");
        Insumo noMinimo = insumo("No mínimo", true, "5");
        Insumo acima = insumo("Acima", true, "5");
        Insumo inativo = insumo("Inativo", false, "5");
        Produto revenda = produto("Revenda", TipoProduto.PRODUTO_REVENDA, true, "2");
        Produto preparacao = produto("Preparação", TipoProduto.PREPARACAO_PRODUZIDA, true, "3");
        produto("Comercial", TipoProduto.PRODUTO_COMERCIAL, true, "10");

        saldo(negativo, null, TipoItemEstoque.INSUMO, "-1", "0");
        saldo(abaixo, null, TipoItemEstoque.INSUMO, "2", "20");
        saldo(noMinimo, null, TipoItemEstoque.INSUMO, "5", "50");
        saldo(acima, null, TipoItemEstoque.INSUMO, "6", "60");
        saldo(inativo, null, TipoItemEstoque.INSUMO, "4", "400");
        saldo(null, revenda, TipoItemEstoque.PRODUTO_REVENDA, "1", "15");
        saldo(null, preparacao, TipoItemEstoque.PREPARACAO_PRODUZIDA, "1", "25");
        em.flush();

        SaldoEstoqueRepository.ResumoDashboard resumo = saldos.resumirDashboard();

        assertThat(resumo.getSemEstoque()).isEqualTo(2); // zero (sem saldo persistido) e negativo
        assertThat(resumo.getAbaixoDoMinimo()).isEqualTo(3); // insumo, revenda e preparação
        assertThat(resumo.getItensComSaldo()).isEqualTo(5); // abaixo, mínimo, acima, revenda e preparação
        assertThat(saldos.listar("", "", "SEM_ESTOQUE", true, PageRequest.of(0, 20)).getContent())
                .extracting(SaldoEstoqueRepository.Visao::getItemNome).containsExactlyInAnyOrder("Zerado", "Negativo");
        assertThat(saldos.listar("", "", "BAIXO", true, PageRequest.of(0, 20)).getContent())
                .extracting(SaldoEstoqueRepository.Visao::getItemNome)
                .containsExactlyInAnyOrder("Abaixo", "Revenda", "Preparação");
        assertThat(saldos.somarValor(TipoItemEstoque.INSUMO)).isEqualByComparingTo("130.00");
        assertThat(saldos.somarValor(TipoItemEstoque.PRODUTO_REVENDA)).isEqualByComparingTo("15.00");
        assertThat(saldos.somarValor(TipoItemEstoque.PREPARACAO_PRODUZIDA)).isEqualByComparingTo("25.00");
        assertThat(List.of(saldos.somarValor(TipoItemEstoque.INSUMO),
                        saldos.somarValor(TipoItemEstoque.PRODUTO_REVENDA),
                        saldos.somarValor(TipoItemEstoque.PREPARACAO_PRODUZIDA)).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("170.00");
    }

    @Test
    void ultimasComprasRetornamNoMaximoCincoAtivasEmOrdemDecrescenteComMista() {
        Insumo insumo = insumo("Feijão", true, "0");
        Produto revenda = produto("Refrigerante", TipoProduto.PRODUTO_REVENDA, true, "0");
        for (int dia = 1; dia <= 6; dia++) compra(LocalDate.of(2026, 8, dia), StatusCompra.ATIVA,
                "Fornecedor " + dia, insumo, revenda, dia == 6);
        compra(LocalDate.of(2026, 8, 7), StatusCompra.ESTORNADA, "Estornada", insumo, revenda, false);
        em.flush();

        List<CompraRepository.CompraRecente> resultado = compras.buscarUltimasAtivas(PageRequest.of(0, 5));

        assertThat(resultado).hasSize(5);
        assertThat(resultado).extracting(CompraRepository.CompraRecente::getDataCompra)
                .containsExactly(LocalDate.of(2026, 8, 6), LocalDate.of(2026, 8, 5),
                        LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 2));
        assertThat(resultado).extracting(CompraRepository.CompraRecente::getFornecedor).doesNotContain("Estornada");
        assertThat(resultado.getFirst().getTipoCompra()).isEqualTo(TipoCompra.MISTA);
        assertThat(resultado.getFirst().getQuantidadeItens()).isEqualTo(2);
    }

    private Insumo insumo(String nome, boolean ativo, String minimo) {
        Insumo i = Insumo.builder().nome(nome).unidadeMedida(UnidadeMedida.QUILOGRAMA).ativo(ativo)
                .estoqueMinimo(new BigDecimal(minimo)).build();
        em.persist(i);
        return i;
    }

    private Produto produto(String nome, TipoProduto tipo, boolean ativo, String minimo) {
        Produto p = Produto.builder().nome(nome).tipoProduto(tipo).unidadeVenda(UnidadeVenda.UNIDADE).ativo(ativo)
                .estoqueMinimo(new BigDecimal(minimo)).build();
        em.persist(p);
        return p;
    }

    private void saldo(Insumo insumo, Produto produto, TipoItemEstoque tipo, String quantidade, String valor) {
        em.persist(SaldoEstoque.builder().insumo(insumo).produto(produto).tipoItem(tipo)
                .quantidadeAtual(new BigDecimal(quantidade)).custoMedioAtual(BigDecimal.ZERO)
                .valorTotalEstoque(new BigDecimal(valor)).build());
    }

    private void compra(LocalDate data, StatusCompra status, String fornecedor, Insumo insumo,
                        Produto revenda, boolean mista) {
        Compra compra = Compra.builder().dataCompra(data).status(status).fornecedor(fornecedor).build();
        compra.adicionarItem(ItemCompra.builder().insumo(insumo).nomeHistorico(insumo.getNome())
                .unidadeHistorica(UnidadeMedida.QUILOGRAMA).quantidade(BigDecimal.ONE)
                .valorTotalItem(BigDecimal.TEN).custoUnitario(BigDecimal.TEN).build());
        if (mista) compra.adicionarItem(ItemCompra.builder().produto(revenda).nomeHistorico(revenda.getNome())
                .unidadeHistorica(UnidadeMedida.UNIDADE).quantidade(BigDecimal.ONE)
                .valorTotalItem(BigDecimal.TEN).custoUnitario(BigDecimal.TEN).build());
        em.persist(compra);
    }
}
