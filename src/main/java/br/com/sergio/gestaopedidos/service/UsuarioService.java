package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.usuario.UsuarioRequest;
import br.com.sergio.gestaopedidos.dto.usuario.UsuarioResponse;
import br.com.sergio.gestaopedidos.dto.usuario.AlterarSenhaInicialRequest;
import br.com.sergio.gestaopedidos.entity.Usuario;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import br.com.sergio.gestaopedidos.mapper.UsuarioMapper;
import br.com.sergio.gestaopedidos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarPaginado(
            String filtro,
            Pageable pageable
    ) {
        String filtroTratado = filtro == null ? "" : filtro.trim();

        return usuarioRepository
                .findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        filtroTratado,
                        filtroTratado,
                        pageable
                )
                .map(usuarioMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        return usuarioMapper.toResponse(usuario);
    }

    public UsuarioResponse salvar(UsuarioRequest request) {
        validarEmailDuplicado(request.email());

        Usuario usuario = usuarioMapper.toEntity(request);

        usuario.setSenha(passwordEncoder.encode(request.senha()));

        if (usuario.getAtivo() == null) {
            usuario.setAtivo(true);
        }

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        return usuarioMapper.toResponse(usuarioSalvo);
    }

    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscarEntidadePorId(id);

        validarEmailNaAtualizacao(usuario, request.email());

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(request.perfil());

        if (request.ativo() != null) {
            usuario.setAtivo(request.ativo());
        }

        Usuario usuarioAtualizado = usuarioRepository.save(usuario);

        return usuarioMapper.toResponse(usuarioAtualizado);
    }

    public void excluir(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        usuarioRepository.delete(usuario);
    }

    public void ativar(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        usuario.setAtivo(true);
    }

    public void inativar(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        usuario.setAtivo(false);
    }

    @Transactional(readOnly = true)
    public boolean deveTrocarSenhaNoPrimeiroAcesso(String email) {
        return Boolean.TRUE.equals(
                buscarEntidadePorEmail(email).getTrocarSenhaPrimeiroAcesso()
        );
    }

    public void alterarSenhaInicial(
            String email,
            AlterarSenhaInicialRequest request
    ) {
        Usuario usuario = buscarEntidadePorEmail(email);

        if (!Boolean.TRUE.equals(usuario.getTrocarSenhaPrimeiroAcesso())) {
            throw new BusinessException("A troca de senha inicial não é mais necessária.");
        }

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new BusinessException("Senha atual incorreta.");
        }

        if (request.novaSenha() == null || request.novaSenha().length() < 6) {
            throw new BusinessException("Nova senha deve ter no mínimo 6 caracteres.");
        }

        if (!request.novaSenha().equals(request.confirmarNovaSenha())) {
            throw new BusinessException("A confirmação da nova senha não coincide.");
        }

        if ("admin".equals(request.novaSenha())) {
            throw new BusinessException("A nova senha não pode ser igual à senha inicial.");
        }

        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenha())) {
            throw new BusinessException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setTrocarSenhaPrimeiroAcesso(false);
        usuarioRepository.save(usuario);
    }

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuário não encontrado."
                        )
                );
    }

    private Usuario buscarEntidadePorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private void validarEmailDuplicado(String email) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException(
                    "Já existe um usuário cadastrado com esse e-mail."
            );
        }
    }

    private void validarEmailNaAtualizacao(
            Usuario usuario,
            String novoEmail
    ) {
        boolean emailFoiAlterado =
                !usuario.getEmail().equalsIgnoreCase(novoEmail);

        if (emailFoiAlterado
                && usuarioRepository.existsByEmailIgnoreCase(novoEmail)) {
            throw new BusinessException(
                    "Já existe um usuário cadastrado com esse e-mail."
            );
        }
    }
}
