package br.com.sergio.gestaopedidos.config;

import br.com.sergio.gestaopedidos.entity.Usuario;
import br.com.sergio.gestaopedidos.enums.PerfilUsuario;
import br.com.sergio.gestaopedidos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioInicializadorTest {

    @Test
    void criaAdministradorSomenteQuandoBancoEstaVazio() throws Exception {
        List<Usuario> usuarios = new ArrayList<>();
        UsuarioRepository repository = repository(usuarios);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        UsuarioInicializador inicializador = new UsuarioInicializador(repository, encoder);

        inicializador.run(null);

        assertThat(usuarios).hasSize(1);
        Usuario administrador = usuarios.getFirst();
        assertThat(administrador.getNome()).isEqualTo("Administrador");
        assertThat(administrador.getEmail()).isEqualTo("admin");
        assertThat(administrador.getSenha()).isNotEqualTo("admin");
        assertThat(encoder.matches("admin", administrador.getSenha())).isTrue();
        assertThat(administrador.getPerfil()).isEqualTo(PerfilUsuario.ADMIN);
        assertThat(administrador.getAtivo()).isTrue();
        assertThat(administrador.getTrocarSenhaPrimeiroAcesso()).isFalse();

        inicializador.run(null);
        assertThat(usuarios).hasSize(1);
    }

    @Test
    void naoAlteraBancoQuandoJaExisteUsuario() throws Exception {
        Usuario existente = Usuario.builder().id(7L).nome("Existente").email("existente")
                .senha("hash-existente").perfil(PerfilUsuario.ATENDENTE).ativo(true).build();
        List<Usuario> usuarios = new ArrayList<>(List.of(existente));
        UsuarioInicializador inicializador = new UsuarioInicializador(repository(usuarios), new BCryptPasswordEncoder());

        inicializador.run(null);

        assertThat(usuarios).containsExactly(existente);
        assertThat(existente.getSenha()).isEqualTo("hash-existente");
    }

    private UsuarioRepository repository(List<Usuario> usuarios) {
        return (UsuarioRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{UsuarioRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "count" -> (long) usuarios.size();
                    case "save" -> {
                        Usuario usuario = (Usuario) args[0];
                        usuarios.add(usuario);
                        yield usuario;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
