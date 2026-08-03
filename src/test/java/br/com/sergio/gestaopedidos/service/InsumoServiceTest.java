package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.insumo.*;
import br.com.sergio.gestaopedidos.entity.Insumo;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.mapper.InsumoMapper;
import br.com.sergio.gestaopedidos.repository.InsumoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class InsumoServiceTest {
    @Test void deveCadastrarComDefaultsENormalizarTextos() {
        Fake f = new Fake();
        InsumoResponse resposta = f.service().salvar(request("  Feijão  ", UnidadeMedida.QUILOGRAMA, null, null));
        assertThat(resposta.nome()).isEqualTo("Feijão");
        assertThat(resposta.ativo()).isTrue();
        assertThat(resposta.estoqueMinimo()).isEqualByComparingTo("0.000");
        assertThat(f.entidade.getDescricao()).isNull();
    }

    @Test void deveRejeitarNomeVazioEUnidadeAusente() {
        Fake f = new Fake();
        assertThatThrownBy(() -> f.service().salvar(request("   ", UnidadeMedida.UNIDADE, "0", true)))
                .isInstanceOf(BusinessException.class).hasMessage("Nome é obrigatório.");
        assertThatThrownBy(() -> f.service().salvar(request("Arroz", null, "0", true)))
                .isInstanceOf(BusinessException.class).hasMessage("Unidade de medida é obrigatória.");
    }

    @Test void deveValidarEstoqueMinimoNegativoEQuantidadeInteiraParaUnidade() {
        Fake f = new Fake();
        assertThatThrownBy(() -> f.service().salvar(request("Óleo", UnidadeMedida.LITRO, "-0.001", true)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> f.service().salvar(request("Embalagem", UnidadeMedida.UNIDADE, "1.500", true)))
                .isInstanceOf(BusinessException.class).hasMessage("Estoque mínimo em unidade deve ser um número inteiro.");
    }

    @Test void deveAceitarTresCasasNasUnidadesDePesoEVolume() {
        for (UnidadeMedida unidade : List.of(UnidadeMedida.QUILOGRAMA, UnidadeMedida.GRAMA,
                UnidadeMedida.LITRO, UnidadeMedida.MILILITRO)) {
            Fake f = new Fake();
            assertThat(f.service().salvar(request("Insumo " + unidade, unidade, "1.125", true)).estoqueMinimo())
                    .isEqualByComparingTo("1.125");
        }
    }

    @Test void deveBloquearDuplicidadeIgnorandoMaiusculasEMinusculas() {
        Fake f = new Fake(); f.duplicado = true;
        assertThatThrownBy(() -> f.service().salvar(request("feijão", UnidadeMedida.QUILOGRAMA, "1", true)))
                .isInstanceOf(BusinessException.class).hasMessage("Já existe um insumo cadastrado com esse nome.");
        assertThat(f.nomeConsultado).isEqualTo("feijão");
    }

    @Test void deveAtualizarMantendoNomeEDataDeCriacao() {
        Fake f = new Fake(); f.entidade = entidade(1L, "Arroz", true);
        var criadoEm = java.time.LocalDateTime.of(2026, 8, 1, 10, 0); f.entidade.setCriadoEm(criadoEm);
        InsumoResponse resposta = f.service().atualizar(1L, request("Arroz", UnidadeMedida.QUILOGRAMA, "2.500", true));
        assertThat(resposta.estoqueMinimo()).isEqualByComparingTo("2.500");
        assertThat(f.entidade.getCriadoEm()).isEqualTo(criadoEm);
    }

    @Test void deveBloquearNomeDuplicadoNaAtualizacao() {
        Fake f = new Fake(); f.entidade = entidade(1L, "Arroz", true); f.duplicadoAtualizacao = true;
        assertThatThrownBy(() -> f.service().atualizar(1L, request("Feijão", UnidadeMedida.QUILOGRAMA, "1", true)))
                .isInstanceOf(BusinessException.class);
    }

    @Test void deveAtivarInativarEExcluir() {
        Fake f = new Fake(); f.entidade = entidade(1L, "Couve", true);
        f.service().inativar(1L); assertThat(f.entidade.getAtivo()).isFalse();
        f.service().ativar(1L); assertThat(f.entidade.getAtivo()).isTrue();
        f.service().excluir(1L); assertThat(f.excluido).isTrue();
    }

    @Test void devePaginarPesquisarPorNomeOuDescricaoEFiltrarStatus() {
        Fake f = new Fake(); f.entidade = entidade(1L, "Carne seca", false); f.total = 25;
        PageRequest pagina = PageRequest.of(1, 10, Sort.by("nome"));
        Page<InsumoResponse> resultado = f.service().listar("  carne  ", false, pagina);
        assertThat(resultado.getNumber()).isEqualTo(1);
        assertThat(resultado.getTotalElements()).isEqualTo(25);
        assertThat(f.filtro).isEqualTo("carne"); assertThat(f.ativo).isFalse();
        assertThat(resultado.getContent().getFirst().nome()).isEqualTo("Carne seca");
    }

    private InsumoRequest request(String nome, UnidadeMedida unidade, String minimo, Boolean ativo) {
        return InsumoRequest.builder().nome(nome).descricao("  ").unidadeMedida(unidade).ativo(ativo)
                .estoqueMinimo(minimo == null ? null : new BigDecimal(minimo)).observacao("  observação  ").build();
    }

    private Insumo entidade(Long id, String nome, boolean ativo) {
        return Insumo.builder().id(id).nome(nome).unidadeMedida(UnidadeMedida.QUILOGRAMA).ativo(ativo)
                .estoqueMinimo(BigDecimal.ZERO.setScale(3)).build();
    }

    private class Fake implements InvocationHandler {
        Insumo entidade; boolean duplicado, duplicadoAtualizacao, excluido; long total = 1;
        String nomeConsultado, filtro; Boolean ativo; InsumoRepository repository;

        InsumoService service() {
            ClassLoader loader = getClass().getClassLoader();
            repository = (InsumoRepository) Proxy.newProxyInstance(loader, new Class[]{InsumoRepository.class}, this);
            InsumoMapper mapper = (InsumoMapper) Proxy.newProxyInstance(loader, new Class[]{InsumoMapper.class}, this);
            return new InsumoService(repository, mapper);
        }

        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "existsByNomeIgnoreCase" -> { nomeConsultado = (String) args[0]; yield duplicado; }
                case "existsByNomeIgnoreCaseAndIdNot" -> duplicadoAtualizacao;
                case "save" -> { entidade = (Insumo) args[0]; if (entidade.getId() == null) entidade.setId(1L); yield entidade; }
                case "findById" -> Optional.ofNullable(entidade);
                case "delete" -> { excluido = true; yield null; }
                case "buscar" -> { filtro = (String) args[0]; ativo = (Boolean) args[1]; Pageable p = (Pageable) args[2]; yield new PageImpl<>(entidade == null ? List.of() : List.of(entidade), p, total); }
                case "toEntity" -> { InsumoRequest r = (InsumoRequest) args[0]; yield Insumo.builder().nome(r.nome()).descricao(r.descricao()).unidadeMedida(r.unidadeMedida()).ativo(r.ativo()).estoqueMinimo(r.estoqueMinimo()).observacao(r.observacao()).build(); }
                case "toResponse" -> { Insumo i = (Insumo) args[0]; yield InsumoResponse.builder().id(i.getId()).nome(i.getNome()).descricao(i.getDescricao()).unidadeMedida(i.getUnidadeMedida()).ativo(i.getAtivo()).estoqueMinimo(i.getEstoqueMinimo()).observacao(i.getObservacao()).criadoEm(i.getCriadoEm()).atualizadoEm(i.getAtualizadoEm()).build(); }
                case "atualizar" -> { InsumoRequest r = (InsumoRequest) args[0]; Insumo i = (Insumo) args[1]; i.setNome(r.nome()); i.setDescricao(r.descricao()); i.setUnidadeMedida(r.unidadeMedida()); i.setAtivo(r.ativo()); i.setEstoqueMinimo(r.estoqueMinimo()); i.setObservacao(r.observacao()); yield null; }
                default -> null;
            };
        }
    }
}
