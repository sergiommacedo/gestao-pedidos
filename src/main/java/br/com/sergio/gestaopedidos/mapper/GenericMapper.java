package br.com.sergio.gestaopedidos.mapper;

public interface GenericMapper<E, REQUEST, RESPONSE> {

    E toEntity(REQUEST request);

    RESPONSE toResponse(E entity);

}