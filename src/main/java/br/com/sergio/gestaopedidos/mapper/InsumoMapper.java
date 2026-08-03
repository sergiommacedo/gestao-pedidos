package br.com.sergio.gestaopedidos.mapper;

import br.com.sergio.gestaopedidos.dto.insumo.*;
import br.com.sergio.gestaopedidos.entity.Insumo;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InsumoMapper extends GenericMapper<Insumo, InsumoRequest, InsumoResponse> {
    @Override
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    Insumo toEntity(InsumoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "criadoEm", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    void atualizar(InsumoRequest request, @MappingTarget Insumo insumo);
}
