package br.com.sergio.gestaopedidos.enums;

public enum TemaSistema {

    MARROM("Marrom", "tema-marrom"),
    AZUL("Azul", "tema-azul"),
    VERDE("Verde", "tema-verde"),
    VINHO("Vinho", "tema-vinho"),
    ROXO("Roxo", "tema-roxo");

    private final String descricao;
    private final String identificadorCss;

    TemaSistema(String descricao, String identificadorCss) {
        this.descricao = descricao;
        this.identificadorCss = identificadorCss;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getIdentificadorCss() {
        return identificadorCss;
    }
}
