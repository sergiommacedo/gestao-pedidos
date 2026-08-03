package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.insumo.*;
import br.com.sergio.gestaopedidos.entity.Insumo;
import br.com.sergio.gestaopedidos.enums.UnidadeMedida;
import br.com.sergio.gestaopedidos.exception.*;
import br.com.sergio.gestaopedidos.mapper.InsumoMapper;
import br.com.sergio.gestaopedidos.repository.InsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InsumoService {
    private static final String MENSAGEM_DUPLICIDADE = "Já existe um insumo cadastrado com esse nome.";
    private final InsumoRepository insumoRepository;
    private final InsumoMapper insumoMapper;

    @Transactional(readOnly = true)
    public Page<InsumoResponse> listar(String filtro, Boolean ativo, Pageable pageable) {
        return insumoRepository.buscar(normalizarFiltro(filtro), ativo, pageable).map(insumoMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public InsumoResponse buscarPorId(Long id) {
        return insumoMapper.toResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<InsumoResponse> buscarAtivosPorNome(String termo) {
        return insumoRepository.findTop20ByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(normalizarFiltro(termo))
                .stream().map(insumoMapper::toResponse).toList();
    }

    public InsumoResponse salvar(InsumoRequest request) {
        DadosNormalizados dados = normalizarEValidar(request);
        if (insumoRepository.existsByNomeIgnoreCase(dados.nome())) duplicado();
        Insumo insumo = insumoMapper.toEntity(request);
        aplicarNormalizados(insumo, dados, request.ativo() == null ? true : request.ativo());
        return insumoMapper.toResponse(insumoRepository.save(insumo));
    }

    public InsumoResponse atualizar(Long id, InsumoRequest request) {
        Insumo insumo = buscarEntidade(id);
        Boolean ativoAtual = insumo.getAtivo();
        DadosNormalizados dados = normalizarEValidar(request);
        if (insumoRepository.existsByNomeIgnoreCaseAndIdNot(dados.nome(), id)) duplicado();
        insumoMapper.atualizar(request, insumo);
        aplicarNormalizados(insumo, dados, request.ativo() == null ? ativoAtual : request.ativo());
        return insumoMapper.toResponse(insumoRepository.save(insumo));
    }

    public void ativar(Long id) { buscarEntidade(id).setAtivo(true); }
    public void inativar(Long id) { buscarEntidade(id).setAtivo(false); }

    /** Exclusão física permitida enquanto Insumo ainda não possui dependências. */
    public void excluir(Long id) { insumoRepository.delete(buscarEntidade(id)); }

    @Transactional(readOnly = true)
    Insumo buscarEntidade(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Insumo não encontrado."));
    }

    private DadosNormalizados normalizarEValidar(InsumoRequest request) {
        String nome = normalizarObrigatorio(request.nome());
        if (nome.isBlank()) throw new BusinessException("Nome é obrigatório.");
        if (request.unidadeMedida() == null) throw new BusinessException("Unidade de medida é obrigatória.");
        BigDecimal minimo = request.estoqueMinimo() == null ? BigDecimal.ZERO : request.estoqueMinimo();
        if (minimo.signum() < 0) throw new BusinessException("Estoque mínimo não pode ser negativo.");
        if (minimo.scale() > 3 && minimo.stripTrailingZeros().scale() > 3)
            throw new BusinessException("Estoque mínimo deve ter no máximo três casas decimais.");
        if (request.unidadeMedida() == UnidadeMedida.UNIDADE && minimo.stripTrailingZeros().scale() > 0)
            throw new BusinessException("Estoque mínimo em unidade deve ser um número inteiro.");
        return new DadosNormalizados(nome, normalizarOpcional(request.descricao()),
                minimo.setScale(3, RoundingMode.UNNECESSARY), normalizarOpcional(request.observacao()));
    }

    private void aplicarNormalizados(Insumo insumo, DadosNormalizados dados, Boolean ativo) {
        insumo.setNome(dados.nome()); insumo.setDescricao(dados.descricao());
        insumo.setEstoqueMinimo(dados.estoqueMinimo()); insumo.setObservacao(dados.observacao());
        insumo.setAtivo(ativo == null ? true : ativo);
    }

    private String normalizarObrigatorio(String valor) { return valor == null ? "" : valor.trim(); }
    private String normalizarOpcional(String valor) { return valor == null || valor.trim().isEmpty() ? null : valor.trim(); }
    private String normalizarFiltro(String filtro) { return filtro == null ? "" : filtro.trim(); }
    private void duplicado() { throw new BusinessException(MENSAGEM_DUPLICIDADE); }
    private record DadosNormalizados(String nome, String descricao, BigDecimal estoqueMinimo, String observacao) {}
}
