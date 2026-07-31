package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.configuracao.ConfiguracaoEmpresaRequest;
import br.com.sergio.gestaopedidos.dto.configuracao.ConfiguracaoEmpresaResponse;
import br.com.sergio.gestaopedidos.entity.ConfiguracaoEmpresa;
import br.com.sergio.gestaopedidos.enums.TemaSistema;
import br.com.sergio.gestaopedidos.repository.ConfiguracaoEmpresaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@Service
@Validated
@RequiredArgsConstructor
public class ConfiguracaoEmpresaService {

    private static final Long CONFIGURACAO_ID = 1L;
    private static final String NOME_EMPRESA_PADRAO = "Feijoada da Vovó Dan";
    private static final String NOME_CURTO_PADRAO = "Vovó Dan";
    private static final String TEXTO_BOAS_VINDAS_PADRAO =
            "Bem-vindo ao sistema da Feijoada da Vovó Dan.";
    private static final String LOGO_PADRAO = "/img/logo-feijoada-vovo-dan.png";

    private final ConfiguracaoEmpresaRepository configuracaoEmpresaRepository;
    private final LogoEmpresaStorageService logoEmpresaStorageService;
    private final Object bloqueioCache = new Object();

    private volatile ConfiguracaoEmpresaResponse configuracaoEmCache;

    @Transactional
    public ConfiguracaoEmpresaResponse buscarConfiguracao() {
        ConfiguracaoEmpresaResponse configuracaoCacheada = configuracaoEmCache;

        if (configuracaoCacheada != null) {
            return configuracaoCacheada;
        }

        synchronized (bloqueioCache) {
            if (configuracaoEmCache == null) {
                ConfiguracaoEmpresa configuracao = configuracaoEmpresaRepository
                        .findById(CONFIGURACAO_ID)
                        .orElseGet(this::criarConfiguracaoPadrao);
                configuracaoEmCache = converterParaResponse(configuracao);
            }

            return configuracaoEmCache;
        }
    }

    @Transactional
    public ConfiguracaoEmpresaResponse salvarOuAtualizar(
            @Valid ConfiguracaoEmpresaRequest request
    ) {
        return salvarOuAtualizar(request, null, false);
    }

    @Transactional
    public ConfiguracaoEmpresaResponse salvarOuAtualizar(
            @Valid ConfiguracaoEmpresaRequest request,
            MultipartFile novaLogo,
            boolean removerLogo
    ) {
        ConfiguracaoEmpresa configuracao = configuracaoEmpresaRepository
                .findById(CONFIGURACAO_ID)
                .orElseGet(this::novaConfiguracaoPadrao);

        configuracao.setNomeEmpresa(request.nomeEmpresa().trim());
        configuracao.setNomeCurto(request.nomeCurto().trim());
        configuracao.setTema(request.tema());
        configuracao.setTextoBoasVindas(normalizarTextoOpcional(request.textoBoasVindas()));

        String logoAnterior = configuracao.getLogoArquivo();
        String logoNova = null;

        try {
            if (novaLogo != null && !novaLogo.isEmpty()) {
                logoNova = logoEmpresaStorageService.armazenar(novaLogo);
                configuracao.setLogoArquivo(logoNova);
            } else if (removerLogo) {
                configuracao.setLogoArquivo(null);
            }

            ConfiguracaoEmpresa configuracaoSalva =
                    configuracaoEmpresaRepository.saveAndFlush(configuracao);
            ConfiguracaoEmpresaResponse response = converterParaResponse(configuracaoSalva);
            configuracaoEmCache = response;

            if (logoAnterior != null && !logoAnterior.equals(configuracao.getLogoArquivo())) {
                logoEmpresaStorageService.remover(logoAnterior);
            }

            return response;
        } catch (RuntimeException exception) {
            if (logoNova != null) {
                logoEmpresaStorageService.remover(logoNova);
            }
            throw exception;
        }
    }

    private ConfiguracaoEmpresa criarConfiguracaoPadrao() {
        return configuracaoEmpresaRepository.saveAndFlush(novaConfiguracaoPadrao());
    }

    private ConfiguracaoEmpresa novaConfiguracaoPadrao() {
        return ConfiguracaoEmpresa.builder()
                .id(CONFIGURACAO_ID)
                .nomeEmpresa(NOME_EMPRESA_PADRAO)
                .nomeCurto(NOME_CURTO_PADRAO)
                .tema(TemaSistema.MARROM)
                .textoBoasVindas(TEXTO_BOAS_VINDAS_PADRAO)
                .build();
    }

    private ConfiguracaoEmpresaResponse converterParaResponse(
            ConfiguracaoEmpresa configuracao
    ) {
        String logoArquivo = normalizarTextoOpcional(configuracao.getLogoArquivo());

        return new ConfiguracaoEmpresaResponse(
                configuracao.getId(),
                valorOuPadrao(configuracao.getNomeEmpresa(), NOME_EMPRESA_PADRAO),
                valorOuPadrao(configuracao.getNomeCurto(), NOME_CURTO_PADRAO),
                configuracao.getTema() == null ? TemaSistema.MARROM : configuracao.getTema(),
                (configuracao.getTema() == null ? TemaSistema.MARROM : configuracao.getTema())
                        .getIdentificadorCss(),
                valorOuPadrao(configuracao.getTextoBoasVindas(), TEXTO_BOAS_VINDAS_PADRAO),
                logoArquivo,
                logoEmpresaStorageService.obterUrlOuPadrao(logoArquivo, LOGO_PADRAO)
        );
    }

    private String valorOuPadrao(String valor, String padrao) {
        String valorTratado = normalizarTextoOpcional(valor);
        return valorTratado == null ? padrao : valorTratado;
    }

    private String normalizarTextoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
