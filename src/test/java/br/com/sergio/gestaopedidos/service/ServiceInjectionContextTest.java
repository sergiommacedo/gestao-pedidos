package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.controller.ProdutoController;
import br.com.sergio.gestaopedidos.mapper.ProdutoMapper;
import br.com.sergio.gestaopedidos.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ServiceInjectionContextTest {

    @Test
    void springInstanciaProdutoServiceControllerEComposicaoProdutoService() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ProdutoRepository.class, () -> mock(ProdutoRepository.class));
            context.registerBean(ProdutoMapper.class, () -> mock(ProdutoMapper.class));
            context.registerBean(EntityManager.class, () -> mock(EntityManager.class));
            context.registerBean(ComposicaoProdutoRepository.class, () -> mock(ComposicaoProdutoRepository.class));
            context.registerBean(InsumoRepository.class, () -> mock(InsumoRepository.class));
            context.registerBean(SaldoEstoqueRepository.class, () -> mock(SaldoEstoqueRepository.class));
            context.register(ProdutoService.class, ProdutoController.class, ComposicaoProdutoService.class);
            context.refresh();

            assertThat(context.getBean(ProdutoService.class)).isNotNull();
            assertThat(context.getBean(ProdutoController.class)).isNotNull();
            assertThat(context.getBean(ComposicaoProdutoService.class)).isNotNull();
        }
    }

    @Test
    void servicesAlteradosPossuemUmUnicoConstrutorInjetavel() {
        assertConstrutorUnico(ProdutoService.class, 3);
        assertConstrutorUnico(CompraService.class, 4);
        assertConstrutorUnico(EstoqueService.class, 5);
        assertConstrutorUnico(FichaTecnicaService.class, 4);
        assertConstrutorUnico(ComposicaoProdutoService.class, 4);
        assertConstrutorUnico(ProducaoService.class, 6);
        assertConstrutorUnico(PedidoService.class, 5);
        assertConstrutorUnico(DashboardService.class, 6);
    }

    private void assertConstrutorUnico(Class<?> tipo, int dependencias) {
        Constructor<?>[] construtores = tipo.getDeclaredConstructors();
        assertThat(construtores).hasSize(1);
        assertThat(construtores[0].getParameterCount()).isEqualTo(dependencias);
    }
}
