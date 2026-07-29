package br.com.sergio.gestaopedidos.mapper;

import br.com.sergio.gestaopedidos.dto.usuario.UsuarioRequest;
import br.com.sergio.gestaopedidos.dto.usuario.UsuarioResponse;
import br.com.sergio.gestaopedidos.entity.Usuario;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioMapper extends GenericMapper<Usuario, UsuarioRequest, UsuarioResponse> {
}