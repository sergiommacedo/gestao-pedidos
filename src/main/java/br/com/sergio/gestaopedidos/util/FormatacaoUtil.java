package br.com.sergio.gestaopedidos.util;

import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

@Component("formatacao")
public class FormatacaoUtil {

    private static final Locale LOCALE_BR = Locale.of("pt", "BR");

    public String formatarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return "";
        }

        String digitos = telefone.replaceAll("\\D", "");

        if (digitos.length() == 11) {
            return digitos.replaceFirst(
                    "(\\d{2})(\\d{5})(\\d{4})",
                    "($1) $2-$3"
            );
        }

        if (digitos.length() == 10) {
            return digitos.replaceFirst(
                    "(\\d{2})(\\d{4})(\\d{4})",
                    "($1) $2-$3"
            );
        }

        return telefone;
    }

    public String formatarCep(String cep) {
        if (cep == null || cep.isBlank()) {
            return "";
        }

        String digitos = cep.replaceAll("\\D", "");
        return digitos.length() == 8
                ? digitos.replaceFirst("(\\d{5})(\\d{3})", "$1-$2")
                : cep;
    }

    public String formatarMoeda(BigDecimal valor) {
        BigDecimal valorSeguro = valor == null ? BigDecimal.ZERO : valor;
        return NumberFormat.getCurrencyInstance(LOCALE_BR)
                .format(valorSeguro)
                .replace('\u00A0', ' ')
                .replace('\u202F', ' ');
    }

    public String formatarDecimalBrasileiro(BigDecimal valor, int escala) {
        if (valor == null) {
            return "";
        }

        NumberFormat formatador = NumberFormat.getNumberInstance(LOCALE_BR);
        formatador.setMinimumFractionDigits(escala);
        formatador.setMaximumFractionDigits(escala);
        formatador.setGroupingUsed(true);
        return formatador.format(valor.setScale(escala, RoundingMode.HALF_UP));
    }

    public String formatarQuantidade(
            BigDecimal quantidade,
            UnidadeVenda unidadeVenda
    ) {
        if (quantidade == null) {
            return "";
        }

        if (unidadeVenda == UnidadeVenda.QUILOGRAMA) {
            return formatarDecimalBrasileiro(quantidade, 3) + " kg";
        }

        return quantidade.setScale(0, RoundingMode.DOWN).toPlainString();
    }

    public String formatarQuantidadeInput(
            BigDecimal quantidade,
            UnidadeVenda unidadeVenda
    ) {
        if (quantidade == null) {
            return "";
        }

        return unidadeVenda == UnidadeVenda.QUILOGRAMA
                ? formatarDecimalBrasileiro(quantidade, 3)
                : quantidade.setScale(0, RoundingMode.DOWN).toPlainString();
    }
}
