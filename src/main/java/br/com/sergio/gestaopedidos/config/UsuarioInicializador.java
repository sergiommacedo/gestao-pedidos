package br.com.sergio.gestaopedidos.config;

import br.com.sergio.gestaopedidos.entity.Usuario;
import br.com.sergio.gestaopedidos.enums.PerfilUsuario;
import br.com.sergio.gestaopedidos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioInicializador implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario administrador = Usuario.builder()
                .nome("Administrador")
                .email("admin")
                .senha(passwordEncoder.encode("admin"))
                .perfil(PerfilUsuario.ADMIN)
                .ativo(true)
                .trocarSenhaPrimeiroAcesso(false)
                .build();

        usuarioRepository.save(administrador);

        log.info("Usuário administrador inicial criado.");
    }
}
