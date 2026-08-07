package br.com.sergio.gestaopedidos.dto.compra;

import br.com.sergio.gestaopedidos.enums.*;

public record ItemBuscaCompraResponse(Long id,String nome,TipoItemEstoque tipoItem,String categoria,
                                      UnidadeMedida unidade,String unidadeDescricao,String simbolo) {}
