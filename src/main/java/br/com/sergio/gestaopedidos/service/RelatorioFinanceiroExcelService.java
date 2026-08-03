package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RelatorioFinanceiroExcelService {

    public byte[] gerar(
            String empresa, LocalDate inicio, LocalDate fim, LocalDateTime geradoEm,
            RelatorioFinanceiroIndicadoresResponse indicadores,
            List<RelatorioFinanceiroPagamentoResponse> pagamentos,
            List<RelatorioFinanceiroEntregaResponse> entregas,
            List<RelatorioFinanceiroDiaResponse> dias
    ) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Estilos estilos = estilos(workbook);
            criarResumo(workbook.createSheet("Resumo"), estilos, empresa, inicio, fim, geradoEm,
                    indicadores, pagamentos, entregas);
            criarFaturamentoDiario(workbook.createSheet("Faturamento diário"), estilos, dias);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o Excel financeiro.", exception);
        }
    }

    private void criarResumo(
            Sheet sheet, Estilos e, String empresa, LocalDate inicio, LocalDate fim,
            LocalDateTime geradoEm, RelatorioFinanceiroIndicadoresResponse i,
            List<RelatorioFinanceiroPagamentoResponse> pagamentos,
            List<RelatorioFinanceiroEntregaResponse> entregas
    ) {
        mesclar(sheet, 0, 0, 6, empresa, e.titulo());
        mesclar(sheet, 1, 0, 6, "Relatório Financeiro", e.titulo());
        mesclar(sheet, 2, 0, 6, "Período: " + dataTexto(inicio) + " a " + dataTexto(fim), e.texto());
        mesclar(sheet, 3, 0, 6, "Gerado em: " + geradoEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), e.texto());
        String[] rotulos = {"Faturamento bruto", "Produtos", "Taxas", "Pedidos válidos", "Ticket médio", "Cancelados", "Valor cancelado"};
        Row nomes = sheet.createRow(5); Row valores = sheet.createRow(6);
        for (int c = 0; c < rotulos.length; c++) { nomes.createCell(c).setCellValue(rotulos[c]); nomes.getCell(c).setCellStyle(e.negrito()); }
        moeda(valores, 0, i.faturamentoBruto(), e.moeda()); moeda(valores, 1, i.faturamentoProdutos(), e.moeda());
        moeda(valores, 2, i.taxasEntrega(), e.moeda()); valores.createCell(3).setCellValue(i.pedidosValidos());
        moeda(valores, 4, i.ticketMedio(), e.moeda()); valores.createCell(5).setCellValue(i.cancelados());
        moeda(valores, 6, i.valorCancelado(), e.moeda());
        Row melhor = sheet.createRow(8); melhor.createCell(0).setCellValue("Melhor dia"); melhor.getCell(0).setCellStyle(e.negrito());
        melhor.createCell(1).setCellValue(i.melhorDia() == null ? "Nenhum" : dataTexto(i.melhorDia()));
        moeda(melhor, 2, i.melhorDiaValor(), e.moeda());

        int linha = 11;
        linha = tituloSecao(sheet, linha, "Por forma de pagamento", e.titulo());
        String[] colunasPagamento = {"Forma", "Pedidos", "Faturamento", "Participação", "Ticket médio"};
        cabecalho(sheet, linha, colunasPagamento, e.cabecalho());
        int inicioPagamento = linha++;
        for (var item : pagamentos) {
            Row row = sheet.createRow(linha++); row.createCell(0).setCellValue(item.formaPagamento().getDescricao());
            row.createCell(1).setCellValue(item.pedidos()); moeda(row, 2, item.faturamento(), e.moeda());
            percentual(row, 3, item.participacaoPercentual(), e.percentual()); moeda(row, 4, item.ticketMedio(), e.moeda());
        }
        if (!pagamentos.isEmpty()) sheet.setAutoFilter(new CellRangeAddress(inicioPagamento, linha - 1, 0, 4));

        linha += 2;
        linha = tituloSecao(sheet, linha, "Por tipo de entrega", e.titulo());
        String[] colunasEntrega = {"Tipo", "Pedidos", "Produtos", "Taxas", "Total", "Participação"};
        cabecalho(sheet, linha++, colunasEntrega, e.cabecalho());
        for (var item : entregas) {
            Row row = sheet.createRow(linha++); row.createCell(0).setCellValue(descricao(item.tipoEntrega()));
            row.createCell(1).setCellValue(item.pedidos()); moeda(row, 2, item.faturamentoProdutos(), e.moeda());
            moeda(row, 3, item.taxasEntrega(), e.moeda()); moeda(row, 4, item.faturamentoTotal(), e.moeda());
            percentual(row, 5, item.participacaoPercentual(), e.percentual());
        }
        int[] larguras = {25, 16, 20, 18, 20, 18, 20};
        for (int c = 0; c < larguras.length; c++) sheet.setColumnWidth(c, larguras[c] * 256);
        sheet.createFreezePane(0, 5);
    }

    private void criarFaturamentoDiario(Sheet sheet, Estilos e, List<RelatorioFinanceiroDiaResponse> dias) {
        String[] colunas = {"Data", "Pedidos válidos", "Cancelados", "Produtos", "Taxas", "Total", "Ticket médio"};
        cabecalho(sheet, 0, colunas, e.cabecalho());
        int linha = 1;
        for (var dia : dias) {
            Row row = sheet.createRow(linha++); data(row, 0, dia.data(), e.data());
            row.createCell(1).setCellValue(dia.pedidosValidos()); row.createCell(2).setCellValue(dia.cancelados());
            moeda(row, 3, dia.faturamentoProdutos(), e.moeda()); moeda(row, 4, dia.taxasEntrega(), e.moeda());
            moeda(row, 5, dia.faturamentoTotal(), e.moeda()); moeda(row, 6, dia.ticketMedio(), e.moeda());
        }
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, linha - 1), 0, 6));
        sheet.createFreezePane(0, 1);
        int[] larguras = {16, 18, 14, 18, 18, 18, 18};
        for (int c = 0; c < larguras.length; c++) sheet.setColumnWidth(c, larguras[c] * 256);
    }

    private Estilos estilos(Workbook workbook) {
        Font bold = workbook.createFont(); bold.setBold(true); DataFormat formato = workbook.createDataFormat();
        CellStyle titulo = workbook.createCellStyle(); titulo.setFont(bold);
        CellStyle texto = workbook.createCellStyle(); CellStyle negrito = workbook.createCellStyle(); negrito.setFont(bold);
        CellStyle cabecalho = workbook.createCellStyle(); cabecalho.setFont(bold); cabecalho.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex()); cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        CellStyle moeda = workbook.createCellStyle(); moeda.setDataFormat(formato.getFormat("R$ #,##0.00"));
        CellStyle percentual = workbook.createCellStyle(); percentual.setDataFormat(formato.getFormat("0.00%"));
        CellStyle data = workbook.createCellStyle(); data.setDataFormat(formato.getFormat("dd/mm/yyyy"));
        return new Estilos(titulo, texto, negrito, cabecalho, moeda, percentual, data);
    }

    private int tituloSecao(Sheet sheet, int linha, String titulo, CellStyle estilo) { mesclar(sheet, linha, 0, 6, titulo, estilo); return linha + 1; }
    private void cabecalho(Sheet sheet, int linha, String[] colunas, CellStyle estilo) { Row row = sheet.createRow(linha); for (int c=0;c<colunas.length;c++){row.createCell(c).setCellValue(colunas[c]);row.getCell(c).setCellStyle(estilo);} }
    private void mesclar(Sheet sheet, int linha, int inicio, int fim, String valor, CellStyle estilo) { Row row=sheet.createRow(linha);row.createCell(inicio).setCellValue(valor);row.getCell(inicio).setCellStyle(estilo);sheet.addMergedRegion(new CellRangeAddress(linha,linha,inicio,fim)); }
    private void moeda(Row row, int coluna, java.math.BigDecimal valor, CellStyle estilo) { row.createCell(coluna).setCellValue(valor.doubleValue());row.getCell(coluna).setCellStyle(estilo); }
    private void percentual(Row row, int coluna, java.math.BigDecimal valor, CellStyle estilo) { row.createCell(coluna).setCellValue(valor.doubleValue()/100);row.getCell(coluna).setCellStyle(estilo); }
    private void data(Row row, int coluna, LocalDate valor, CellStyle estilo) { row.createCell(coluna).setCellValue(java.util.Date.from(valor.atStartOfDay(ZoneId.systemDefault()).toInstant()));row.getCell(coluna).setCellStyle(estilo); }
    private String dataTexto(LocalDate data) { return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
    private String descricao(br.com.sergio.gestaopedidos.enums.TipoEntrega tipo) { return tipo == br.com.sergio.gestaopedidos.enums.TipoEntrega.ENTREGA ? "Entrega" : "Retirada"; }
    private record Estilos(CellStyle titulo, CellStyle texto, CellStyle negrito, CellStyle cabecalho, CellStyle moeda, CellStyle percentual, CellStyle data) {}
}
