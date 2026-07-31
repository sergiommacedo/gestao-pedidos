package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LogoEmpresaStorageService {

    private static final long TAMANHO_MAXIMO = 2L * 1024L * 1024L;
    private static final Map<String, Set<String>> TIPOS_PERMITIDOS = Map.of(
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "webp", Set.of("image/webp")
    );

    private final Path diretorio;

    public LogoEmpresaStorageService(
            @Value("${app.upload.logo-dir:./uploads/logos}") String diretorio
    ) {
        this.diretorio = Paths.get(diretorio).toAbsolutePath().normalize();
    }

    @PostConstruct
    void inicializar() {
        try {
            Files.createDirectories(diretorio);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível preparar a pasta de logos.", exception);
        }
    }

    public String armazenar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            return null;
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new BusinessException("A logo deve possuir no máximo 2 MB.");
        }

        String extensao = obterExtensao(arquivo.getOriginalFilename());
        String contentType = arquivo.getContentType() == null
                ? ""
                : arquivo.getContentType().toLowerCase(Locale.ROOT);

        if (!TIPOS_PERMITIDOS.containsKey(extensao)
                || !TIPOS_PERMITIDOS.get(extensao).contains(contentType)) {
            throw new BusinessException("Envie uma logo nos formatos PNG, JPG, JPEG ou WEBP.");
        }

        validarAssinatura(arquivo, extensao);

        String nomeSeguro = UUID.randomUUID() + "." + extensao;
        Path destino = resolverSeguro(nomeSeguro);

        try (InputStream conteudo = arquivo.getInputStream()) {
            Files.copy(conteudo, destino);
            return nomeSeguro;
        } catch (IOException exception) {
            throw new BusinessException("Não foi possível armazenar a logo enviada.");
        }
    }

    public void remover(String nomeArquivo) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return;
        }

        try {
            Files.deleteIfExists(resolverSeguro(nomeArquivo));
        } catch (IllegalArgumentException exception) {
            log.warn("Referência de logo inválida ignorada durante a remoção.");
        } catch (IOException exception) {
            log.warn("Não foi possível remover o arquivo antigo da logo.");
        }
    }

    public String obterUrlOuPadrao(String nomeArquivo, String logoPadrao) {
        if (nomeArquivo == null || nomeArquivo.isBlank()) {
            return logoPadrao;
        }

        try {
            return Files.isRegularFile(resolverSeguro(nomeArquivo))
                    ? "/uploads/logos/" + nomeArquivo
                    : logoPadrao;
        } catch (IllegalArgumentException exception) {
            return logoPadrao;
        }
    }

    public String obterLocalizacaoRecurso() {
        return diretorio.toUri().toString();
    }

    private String obterExtensao(String nomeOriginal) {
        if (nomeOriginal == null) {
            return "";
        }

        String apenasNome = Paths.get(nomeOriginal).getFileName().toString();
        int separador = apenasNome.lastIndexOf('.');
        return separador < 0
                ? ""
                : apenasNome.substring(separador + 1).toLowerCase(Locale.ROOT);
    }

    private Path resolverSeguro(String nomeArquivo) {
        if (!nomeArquivo.equals(Paths.get(nomeArquivo).getFileName().toString())) {
            throw new IllegalArgumentException("Nome de arquivo inválido.");
        }

        Path caminho = diretorio.resolve(nomeArquivo).normalize();
        if (!caminho.startsWith(diretorio)) {
            throw new IllegalArgumentException("Caminho de arquivo inválido.");
        }
        return caminho;
    }

    private void validarAssinatura(MultipartFile arquivo, String extensao) {
        try (InputStream input = arquivo.getInputStream()) {
            byte[] cabecalho = input.readNBytes(12);
            boolean valido = switch (extensao) {
                case "png" -> cabecalho.length >= 8
                        && (cabecalho[0] & 0xff) == 0x89
                        && cabecalho[1] == 0x50 && cabecalho[2] == 0x4e
                        && cabecalho[3] == 0x47;
                case "jpg", "jpeg" -> cabecalho.length >= 3
                        && (cabecalho[0] & 0xff) == 0xff
                        && (cabecalho[1] & 0xff) == 0xd8
                        && (cabecalho[2] & 0xff) == 0xff;
                case "webp" -> cabecalho.length >= 12
                        && cabecalho[0] == 'R' && cabecalho[1] == 'I'
                        && cabecalho[2] == 'F' && cabecalho[3] == 'F'
                        && cabecalho[8] == 'W' && cabecalho[9] == 'E'
                        && cabecalho[10] == 'B' && cabecalho[11] == 'P';
                default -> false;
            };

            if (!valido) {
                throw new BusinessException("O conteúdo do arquivo não corresponde a uma imagem válida.");
            }
        } catch (IOException exception) {
            throw new BusinessException("Não foi possível validar a logo enviada.");
        }
    }
}
