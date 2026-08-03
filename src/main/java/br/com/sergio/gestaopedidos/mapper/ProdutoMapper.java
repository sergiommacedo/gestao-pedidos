package br.com.sergio.gestaopedidos.mapper;

import br.com.sergio.gestaopedidos.dto.produto.ProdutoRequest;
import br.com.sergio.gestaopedidos.dto.produto.ProdutoResponse;
import br.com.sergio.gestaopedidos.entity.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProdutoMapper extends GenericMapper<Produto, ProdutoRequest, ProdutoResponse> {
    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipoProduto", source = "tipoProduto")
    @Mapping(target = "vendavel", source = "vendavel")
    Produto toEntity(ProdutoRequest request);
}
