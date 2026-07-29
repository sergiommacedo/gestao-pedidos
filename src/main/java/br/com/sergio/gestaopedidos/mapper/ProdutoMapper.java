package br.com.sergio.gestaopedidos.mapper;

import br.com.sergio.gestaopedidos.dto.produto.ProdutoRequest;
import br.com.sergio.gestaopedidos.dto.produto.ProdutoResponse;
import br.com.sergio.gestaopedidos.entity.Produto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProdutoMapper extends GenericMapper<Produto, ProdutoRequest, ProdutoResponse> {
}