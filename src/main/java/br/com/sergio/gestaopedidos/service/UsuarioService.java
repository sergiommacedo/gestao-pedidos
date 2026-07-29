package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.usuario.UsuarioRequest;
import br.com.sergio.gestaopedidos.dto.usuario.UsuarioResponse;
import br.com.sergio.gestaopedidos.entity.Usuario;
import br.com.sergio.gestaopedidos.exception.BusinessException;
import br.com.sergio.gestaopedidos.exception.ResourceNotFoundException;
import br.com.sergio.gestaopedidos.mapper.UsuarioMapper;
import br.com.sergio.gestaopedidos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = buscarEntidadePorId(id);
        return usuarioMapper.toResponse(usuario);
    }

    public UsuarioResponse salvar(UsuarioRequest request) {
        validarEmailDuplicado(request.email());

        Usuario usuario = usuarioMapper.toEntity(request);

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
        usuario.setSenha(request.senha());
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

    private Usuario buscarEntidadePorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Usuário não encontrado."
                        )
                );
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