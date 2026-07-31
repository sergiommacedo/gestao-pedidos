package br.com.sergio.gestaopedidos.enums;

public enum FormaPagamento {

    DINHEIRO("Dinheiro"),
    PIX("Pix"),
    CARTAO_CREDITO("Cartão de crédito"),
    CARTAO_DEBITO("Cartão de débito"),
    PAGAMENTO_NA_ENTREGA("Pagamento na entrega"),
    OUTRO("Outro");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}