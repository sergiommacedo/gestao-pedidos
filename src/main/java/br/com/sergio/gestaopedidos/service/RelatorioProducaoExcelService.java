package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioProducaoIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioProducaoLinhaResponse;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class RelatorioProducaoExcelService {

    private static final int CABECALHO_TABELA = 11;
    private static final String[] COLUNAS = {
            "Posição", "Produto", "Unidade", "Quantidade", "Pedidos",
            "Faturamento", "Média por pedido", "Participação"
    };

    public byte[] gerar(
            String empresa,
            LocalDate inicio,
            LocalDate fim,
            LocalDateTime geradoEm,
            List<RelatorioProducaoLinhaResponse> linhas,
            RelatorioProducaoIndicadoresResponse indicadores
    ) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Produção");
            Estilos estilos = estilos(workbook);
            cabecalho(sheet, estilos, empresa, inicio, fim, geradoEm, indicadores);
            tabela(sheet, estilos, linhas);
            sheet.setAutoFilter(new CellRangeAddress(CABECALHO_TABELA, CABECALHO_TABELA, 0, 7));
            sheet.createFreezePane(0, CABECALHO_TABELA + 1);
            int[] larguras = {11, 32, 16, 18, 12, 18, 20, 16};
            for (int i = 0; i < larguras.length; i++) sheet.setColumnWidth(i, larguras[i] * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o Excel de produção.", exception);
        }
    }

    private void cabecalho(
            Sheet sheet, Estilos e, String empresa, LocalDate inicio, LocalDate fim,
            LocalDateTime geradoEm, RelatorioProducaoIndicadoresResponse i
    ) {
        mesclar(sheet, 0, empresa, e.titulo());
        mesclar(sheet, 1, "Relatório de Produção", e.titulo());
        mesclar(sheet, 2, "Período: " + inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                + " a " + fim.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), e.texto());
        mesclar(sheet, 3, "Gerado em: " + geradoEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), e.texto());
        String[] nomes = {"Produtos", "Unidades", "Quilogramas", "Faturamento", "Taxas", "Total geral", "Produto líder"};
        Row rotulos = sheet.createRow(5);
        Row valores = sheet.createRow(6);
        for (int c = 0; c < nomes.length; c++) {
            rotulos.createCell(c).setCellValue(nomes[c]);
            rotulos.getCell(c).setCellStyle(e.negrito());
        }
        valores.createCell(0).setCellValue(i.produtosDistintos());
        valores.createCell(1).setCellValue(i.totalUnidades().doubleValue());
        valores.getCell(1).setCellStyle(e.inteiro());
        valores.createCell(2).setCellValue(i.totalQuilogramas().doubleValue());
        valores.getCell(2).setCellStyle(e.quilo());
        moeda(valores, 3, i.faturamentoProdutos(), e.moeda());
        moeda(valores, 4, i.taxasEntrega(), e.moeda());
        moeda(valores, 5, i.totalGeral(), e.moeda());
        valores.createCell(6).setCellValue(i.produtoLiderNome());
    }

    private void tabela(Sheet sheet, Estilos e, List<RelatorioProducaoLinhaResponse> linhas) {
        Row cabecalho = sheet.createRow(CABECALHO_TABELA);
        for (int c = 0; c < COLUNAS.length; c++) {
            cabecalho.createCell(c).setCellValue(COLUNAS[c]);
            cabecalho.getCell(c).setCellStyle(e.cabecalho());
        }
        int n = CABECALHO_TABELA + 1;
        for (RelatorioProducaoLinhaResponse linha : linhas) {
            Row row = sheet.createRow(n++);
            row.createCell(0).setCellValue(linha.posicao());
            row.createCell(1).setCellValue(linha.produtoNome());
            row.createCell(2).setCellValue(linha.unidadeVenda().getDescricao());
            row.createCell(3).setCellValue(linha.quantidadeTotal().doubleValue());
            row.getCell(3).setCellStyle(linha.unidadeVenda() == UnidadeVenda.UNIDADE ? e.inteiro() : e.quilo());
            row.createCell(4).setCellValue(linha.pedidosDistintos());
            moeda(row, 5, linha.faturamentoTotal(), e.moeda());
            moeda(row, 6, linha.mediaPorPedido(), e.moeda());
            row.createCell(7).setCellValue(linha.participacaoPercentual().doubleValue() / 100);
            row.getCell(7).setCellStyle(e.percentual());
        }
    }

    private Estilos estilos(Workbook workbook) {
        Font bold = workbook.createFont(); bold.setBold(true);
        CellStyle titulo = workbook.createCellStyle(); titulo.setFont(bold);
        CellStyle texto = workbook.createCellStyle();
        CellStyle negrito = workbook.createCellStyle(); negrito.setFont(bold);
        CellStyle cabecalho = workbook.createCellStyle(); cabecalho.setFont(bold);
        cabecalho.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        CellStyle moeda = workbook.createCellStyle(); moeda.setDataFormat(workbook.createDataFormat().getFormat("R$ #,##0.00"));
        CellStyle inteiro = workbook.createCellStyle(); inteiro.setDataFormat(workbook.createDataFormat().getFormat("0"));
        CellStyle quilo = workbook.createCellStyle(); quilo.setDataFormat(workbook.createDataFormat().getFormat("0.000"));
        CellStyle percentual = workbook.createCellStyle(); percentual.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        return new Estilos(titulo, texto, negrito, cabecalho, moeda, inteiro, quilo, percentual);
    }

    private void mesclar(Sheet sheet, int linha, String valor, CellStyle estilo) {
        Row row = sheet.createRow(linha); row.createCell(0).setCellValue(valor); row.getCell(0).setCellStyle(estilo);
        sheet.addMergedRegion(new CellRangeAddress(linha, linha, 0, 7));
    }

    private void moeda(Row row, int coluna, java.math.BigDecimal valor, CellStyle estilo) {
        row.createCell(coluna).setCellValue(valor.doubleValue()); row.getCell(coluna).setCellStyle(estilo);
    }

    private record Estilos(CellStyle titulo, CellStyle texto, CellStyle negrito, CellStyle cabecalho,
                           CellStyle moeda, CellStyle inteiro, CellStyle quilo, CellStyle percentual) {}
}
