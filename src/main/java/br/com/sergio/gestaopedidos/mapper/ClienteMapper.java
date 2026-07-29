package br.com.sergio.gestaopedidos.mapper;

import br.com.sergio.gestaopedidos.dto.cliente.ClienteRequest;
import br.com.sergio.gestaopedidos.dto.cliente.ClienteResponse;
import br.com.sergio.gestaopedidos.entity.Cliente;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ClienteMapper extends GenericMapper<Cliente, ClienteRequest, ClienteResponse> {
}