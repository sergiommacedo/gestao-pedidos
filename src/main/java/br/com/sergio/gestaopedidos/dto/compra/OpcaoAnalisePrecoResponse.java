package br.com.sergio.gestaopedidos.dto.compra;

import br.com.sergio.gestaopedidos.enums.TipoItemEstoque;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;

public record OpcaoAnalisePrecoResponse(TipoItemEstoque tipoItem, Long referenciaId, String nome,
                                        UnidadeMedida unidade, String categoria, String simbolo) {}
