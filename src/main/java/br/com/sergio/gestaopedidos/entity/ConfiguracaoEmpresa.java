package br.com.sergio.gestaopedidos.entity;

import br.com.sergio.gestaopedidos.enums.TemaSistema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracao_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ConfiguracaoEmpresa {

    @Id
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank(message = "Nome da empresa é obrigatório.")
    @Size(max = 150, message = "Nome da empresa deve ter no máximo 150 caracteres.")
    @Column(name = "nome_empresa", nullable = false, length = 150)
    private String nomeEmpresa;

    @NotBlank(message = "Nome curto é obrigatório.")
    @Size(max = 60, message = "Nome curto deve ter no máximo 60 caracteres.")
    @Column(name = "nome_curto", nullable = false, length = 60)
    private String nomeCurto;

    @NotNull(message = "Tema é obrigatório.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TemaSistema tema;

    @Size(max = 255, message = "Texto de boas-vindas deve ter no máximo 255 caracteres.")
    @Column(name = "texto_boas_vindas", length = 255)
    private String textoBoasVindas;

    @Size(max = 255, message = "Arquivo da logo deve ter no máximo 255 caracteres.")
    @Column(name = "logo_arquivo", length = 255)
    private String logoArquivo;
}
