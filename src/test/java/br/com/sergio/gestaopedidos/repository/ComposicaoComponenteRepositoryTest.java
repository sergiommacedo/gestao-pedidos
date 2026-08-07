package br.com.sergio.gestaopedidos.repository;

import br.com.sergio.gestaopedidos.entity.Insumo;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ComposicaoComponenteRepositoryTest {

    @Autowired
    private InsumoRepository insumos;

    @Test
    void retornaTodosOsInsumosAtivosOrdenadosMesmoSemEstoqueCustoCompraOuMovimentacao() {
        List<Insumo> ativos = new ArrayList<>();
        for (int indice = 25; indice >= 1; indice--) {
            ativos.add(insumo("Insumo %02d".formatted(indice), true));
        }
        insumos.saveAll(ativos);
        Insumo inativo = insumos.save(insumo("Insumo inativo", false));

        List<Insumo> encontrados =
                insumos.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("");

        assertThat(encontrados).hasSize(25);
        assertThat(encontrados).extracting(Insumo::getNome).containsExactly(
                "Insumo 01", "Insumo 02", "Insumo 03", "Insumo 04", "Insumo 05",
                "Insumo 06", "Insumo 07", "Insumo 08", "Insumo 09", "Insumo 10",
                "Insumo 11", "Insumo 12", "Insumo 13", "Insumo 14", "Insumo 15",
                "Insumo 16", "Insumo 17", "Insumo 18", "Insumo 19", "Insumo 20",
                "Insumo 21", "Insumo 22", "Insumo 23", "Insumo 24", "Insumo 25"
        );
        assertThat(encontrados).noneMatch(item -> item.getId().equals(inativo.getId()));
    }

    @Test
    void aplicaPesquisaPorNomeSemIntroduzirOutrosFiltrosDeElegibilidade() {
        insumos.saveAll(List.of(
                insumo("Alho nacional", true),
                insumo("Alho picado", true),
                insumo("Cebola", true)
        ));

        assertThat(insumos.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("ALHO"))
                .extracting(Insumo::getNome)
                .containsExactly("Alho nacional", "Alho picado");
    }

    private Insumo insumo(String nome, boolean ativo) {
        return Insumo.builder()
                .nome(nome)
                .unidadeMedida(UnidadeMedida.QUILOGRAMA)
                .estoqueMinimo(BigDecimal.ZERO)
                .ativo(ativo)
                .build();
    }
}
