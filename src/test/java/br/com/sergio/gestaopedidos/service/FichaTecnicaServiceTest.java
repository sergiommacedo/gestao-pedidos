package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.ficha.*;
import br.com.sergio.gestaopedidos.entity.*;
import br.com.sergio.gestaopedidos.enums.*;
import br.com.sergio.gestaopedidos.exception.*;
import br.com.sergio.gestaopedidos.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

class FichaTecnicaServiceTest {

    @Test
    void calculaCustoTotalMargemEIndicaItemSemCusto() {
        Fake f = new Fake(); f.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.UNIDADE, "20.00");
        f.insumo(1, UnidadeMedida.QUILOGRAMA, true); f.insumo(2, UnidadeMedida.UNIDADE, true);
        f.saldo(1, "10", "8");
        var resposta = f.service().salvar(f.request(1, f.item(null, 1, "0.300"), f.item(null, 2, "1")));
        assertThat(resposta.custoEstimadoTotal()).isEqualByComparingTo("2.40");
        assertThat(resposta.custoCompleto()).isFalse();
        assertThat(resposta.quantidadeItensSemCusto()).isEqualTo(1);
        assertThat(resposta.margemContribuicaoEstimada()).isEqualByComparingTo("17.60");
        assertThat(resposta.margemPercentualEstimada()).isEqualByComparingTo("88.00");
    }

    @Test
    void aceitaCustoSemSaldoComoPendenteSemMovimentarEstoque() {
        Fake f = new Fake(); f.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.UNIDADE, "10");
        f.insumo(1, UnidadeMedida.UNIDADE, true);
        var resposta = f.service().salvar(f.request(1, f.item(null, 1, "1")));
        assertThat(resposta.itens().getFirst().possuiCusto()).isFalse();
        assertThat(resposta.itens().getFirst().custoEstimado()).isZero();
        assertThat(f.movimentacoesCriadas).isZero();
    }

    @Test
    void validaRendimentoEsperadoObrigatorioPositivoEComAteTresCasas() {
        Fake f = new Fake(); f.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.QUILOGRAMA, "10");
        f.insumo(1, UnidadeMedida.QUILOGRAMA, true);
        assertThatThrownBy(() -> f.service().salvar(f.request(1, (String) null, f.item(null, 1, "1"))))
                .hasMessageContaining("Informe o rendimento esperado");
        assertThatThrownBy(() -> f.service().salvar(f.request(1, "0", f.item(null, 1, "1"))))
                .hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> f.service().salvar(f.request(1, "-1", f.item(null, 1, "1"))))
                .hasMessageContaining("maior que zero");
        assertThatThrownBy(() -> f.service().salvar(f.request(1, "35.0001", f.item(null, 1, "1"))))
                .hasMessageContaining("três casas decimais");
    }

    @Test
    void aceitaRendimentoDecimalParaKgEExigeInteiroParaUnidade() {
        Fake kg = new Fake(); kg.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.QUILOGRAMA, "10");
        kg.insumo(1, UnidadeMedida.QUILOGRAMA, true);
        assertThat(kg.service().salvar(kg.request(1, "35.125", kg.item(null, 1, "10"))).rendimentoEsperado())
                .isEqualByComparingTo("35.125");

        Fake unidade = new Fake(); unidade.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.UNIDADE, "10");
        unidade.insumo(1, UnidadeMedida.UNIDADE, true);
        assertThat(unidade.service().salvar(unidade.request(1, "35", unidade.item(null, 1, "10"))).rendimentoEsperado())
                .isEqualByComparingTo("35.000");
        assertThatThrownBy(() -> unidade.service().atualizar(1L, unidade.request(1, "35.5", unidade.item(1L, 1, "10"))))
                .hasMessageContaining("número inteiro");
    }

    @Test
    void calculaCustoDaReceitaDeTrintaECincoKgECustoEstimadoPorKg() {
        Fake f = new Fake(); f.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.QUILOGRAMA, "0");
        f.insumo(1, UnidadeMedida.QUILOGRAMA, true); f.insumo(2, UnidadeMedida.LITRO, true);
        f.saldo(1, "100", "8.500000"); f.saldo(2, "10", "6.000000");
        var resposta = f.service().salvar(f.request(1, "35.000", f.item(null, 1, "10"), f.item(null, 2, "1")));
        assertThat(resposta.custoEstimadoTotal()).isEqualByComparingTo("91.00");
        assertThat(resposta.custoEstimadoPorUnidade()).isEqualByComparingTo("2.600000");
    }

    @Test
    void rejeitaProdutoRevendaEProdutoInativo() {
        Fake f = new Fake(); f.insumo(1, UnidadeMedida.UNIDADE, true);
        f.produto(1, TipoProduto.PRODUTO_REVENDA, true, UnidadeVenda.UNIDADE, "10");
        assertThatThrownBy(() -> f.service().salvar(f.request(1, f.item(null, 1, "1"))))
                .hasMessageContaining("preparações produzidas");
        f.produto(2, TipoProduto.PREPARACAO_PRODUZIDA, false, UnidadeVenda.UNIDADE, "10");
        assertThatThrownBy(() -> f.service().salvar(f.request(2, f.item(null, 1, "1"))))
                .hasMessageContaining("não está disponível");
    }

    @Test
    void validaItensDuplicadosVaziosInativosEQuantidadeDeUnidade() {
        Fake f = new Fake(); f.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.UNIDADE, "10");
        f.insumo(1, UnidadeMedida.UNIDADE, true); f.insumo(2, UnidadeMedida.QUILOGRAMA, false);
        assertThatThrownBy(() -> f.service().salvar(f.request(1))).hasMessageContaining("ao menos um");
        assertThatThrownBy(() -> f.service().salvar(f.request(1, f.item(null, 1, "1"), f.item(null, 1, "2"))))
                .hasMessageContaining("já foi adicionado");
        assertThatThrownBy(() -> f.service().salvar(f.request(1, f.item(null, 2, "1"))))
                .hasMessageContaining("não está disponível");
        assertThatThrownBy(() -> f.service().salvar(f.request(1, f.item(null, 1, "1.5"))))
                .hasMessageContaining("inteiro");
        assertThatThrownBy(() -> f.service().salvar(f.request(1, f.item(null, 1, "0"))))
                .hasMessageContaining("maior que zero");
    }

    @Test
    void impedeSegundaFichaDoMesmoProduto() {
        Fake f = new Fake(); f.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.UNIDADE, "10");
        f.insumo(1, UnidadeMedida.UNIDADE, true); f.service().salvar(f.request(1, f.item(null, 1, "1")));
        assertThatThrownBy(() -> f.service().salvar(f.request(1, f.item(null, 1, "1"))))
                .hasMessageContaining("já possui");
    }

    @Test
    void edicaoPreservaInsumoHistoricoInativoERejeitaIdDeOutraFicha() {
        Fake f = new Fake(); f.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.UNIDADE, "10");
        f.insumo(1, UnidadeMedida.UNIDADE, true); f.insumo(2, UnidadeMedida.UNIDADE, true);
        var salva = f.service().salvar(f.request(1, f.item(null, 1, "1")));
        Long itemId = salva.itens().getFirst().id(); f.insumos.get(1L).setAtivo(false);
        var atualizada = f.service().atualizar(salva.id(), f.request(1, f.item(itemId, 1, "2")));
        assertThat(atualizada.itens().getFirst().quantidade()).isEqualByComparingTo("2.000");
        assertThatThrownBy(() -> f.service().atualizar(salva.id(), f.request(1, f.item(999L, 2, "1"))))
                .hasMessageContaining("não pertence");
    }

    @Test
    void ativaInativaExcluiEListaPaginado() {
        Fake f = new Fake(); f.produto(1, TipoProduto.PREPARACAO_PRODUZIDA, true, UnidadeVenda.QUILOGRAMA, "30");
        f.insumo(1, UnidadeMedida.QUILOGRAMA, true);
        Long id = f.service().salvar(f.request(1, f.item(null, 1, "0.125"))).id();
        f.service().inativar(id); assertThat(f.fichas.get(id).getAtiva()).isFalse();
        f.service().ativar(id); assertThat(f.fichas.get(id).getAtiva()).isTrue();
        assertThat(f.service().listar("", null, "", PageRequest.of(0, 10))).hasSize(1);
        f.service().excluir(id); assertThat(f.fichas).isEmpty();
    }

    static class Fake implements InvocationHandler {
        final Map<Long, Produto> produtos = new HashMap<>();
        final Map<Long, Insumo> insumos = new HashMap<>();
        final Map<Long, SaldoEstoque> saldos = new HashMap<>();
        final Map<Long, FichaTecnica> fichas = new LinkedHashMap<>();
        long fichaSeq = 1, itemSeq = 1; int movimentacoesCriadas;

        FichaTecnicaService service() {
            FichaTecnicaRepository fr = (FichaTecnicaRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{FichaTecnicaRepository.class}, this);
            SaldoEstoqueRepository sr = (SaldoEstoqueRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{SaldoEstoqueRepository.class}, (p, m, a) -> switch (m.getName()) {
                        case "buscarSaldo" -> Optional.ofNullable(saldos.get(a[1]));
                        case "buscarSaldosInsumos" -> ((Collection<Long>) a[0]).stream().map(saldos::get).filter(Objects::nonNull).toList();
                        default -> zero(m.getReturnType());
                    });
            ProdutoService ps = new ProdutoService(null, null, null) {
                @Override public Produto buscarEntidadePorId(Long id) { return Optional.ofNullable(produtos.get(id)).orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado.")); }
            };
            InsumoService is = new InsumoService(null, null) {
                @Override Insumo buscarEntidade(Long id) { return Optional.ofNullable(insumos.get(id)).orElseThrow(() -> new ResourceNotFoundException("Insumo não encontrado.")); }
            };
            return new FichaTecnicaService(fr, ps, is, sr);
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsByProdutoId" -> fichas.values().stream().anyMatch(f -> f.getProduto().getId().equals(args[0]));
                case "findByProdutoId" -> fichas.values().stream().filter(f -> f.getProduto().getId().equals(args[0])).findFirst();
                case "buscarDetalhada" -> Optional.ofNullable(fichas.get(args[0]));
                case "buscarDetalhadas" -> ((Collection<Long>) args[0]).stream().map(fichas::get).filter(Objects::nonNull).toList();
                case "save" -> { FichaTecnica f = (FichaTecnica) args[0]; if (f.getId() == null) f.setId(fichaSeq++); for (var i : f.getItens()) if (i.getId() == null) i.setId(itemSeq++); fichas.put(f.getId(), f); yield f; }
                case "delete" -> { fichas.remove(((FichaTecnica) args[0]).getId()); yield null; }
                case "listar" -> new PageImpl<>(new ArrayList<>(fichas.values()), (Pageable) args[3], fichas.size());
                default -> zero(method.getReturnType());
            };
        }

        void produto(long id, TipoProduto tipo, boolean ativo, UnidadeVenda unidade, String preco) { produtos.put(id, Produto.builder().id(id).nome("Produto " + id).tipoProduto(tipo).ativo(ativo).unidadeVenda(unidade).preco(new BigDecimal(preco)).build()); }
        void insumo(long id, UnidadeMedida unidade, boolean ativo) { insumos.put(id, Insumo.builder().id(id).nome("Insumo " + id).unidadeMedida(unidade).ativo(ativo).build()); }
        void saldo(long id, String quantidade, String custo) { saldos.put(id, SaldoEstoque.builder().tipoItem(TipoItemEstoque.INSUMO).insumo(insumos.get(id)).quantidadeAtual(new BigDecimal(quantidade)).custoMedioAtual(new BigDecimal(custo)).build()); }
        FichaTecnicaRequest request(long produto, ItemFichaTecnicaRequest... itens) { return request(produto, "1", itens); }
        FichaTecnicaRequest request(long produto, String rendimento, ItemFichaTecnicaRequest... itens) { return FichaTecnicaRequest.builder().produtoId(produto).rendimentoEsperado(rendimento == null ? null : new BigDecimal(rendimento)).ativa(true).itens(new ArrayList<>(List.of(itens))).build(); }
        ItemFichaTecnicaRequest item(Long id, long insumo, String quantidade) { return ItemFichaTecnicaRequest.builder().id(id).insumoId(insumo).quantidade(new BigDecimal(quantidade)).build(); }
        static Object zero(Class<?> type) { if (type == boolean.class) return false; if (type == long.class) return 0L; if (Optional.class.isAssignableFrom(type)) return Optional.empty(); return null; }
    }
}
