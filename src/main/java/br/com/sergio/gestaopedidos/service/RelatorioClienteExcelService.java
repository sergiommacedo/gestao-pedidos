package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioClienteIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioClienteLinhaResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import br.com.sergio.gestaopedidos.util.FormatacaoUtil;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioClienteExcelService {

    private static final int CABECALHO_TABELA = 11;
    private static final String[] COLUNAS = {
            "Posição", "Cliente", "Telefone", "Pedidos", "Faturamento", "Ticket médio",
            "Primeira compra", "Última compra", "Entregas", "Retiradas", "Participação"
    };

    private final FormatacaoUtil formatacaoUtil;

    public byte[] gerar(
            String empresa,
            LocalDate inicio,
            LocalDate fim,
            LocalDateTime geradoEm,
            List<RelatorioClienteLinhaResponse> linhas,
            RelatorioClienteIndicadoresResponse indicadores
    ) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Clientes");
            Estilos estilos = criarEstilos(workbook);
            criarCabecalho(sheet, estilos, empresa, inicio, fim, geradoEm, indicadores);
            criarTabela(sheet, estilos, linhas);
            sheet.setAutoFilter(new CellRangeAddress(CABECALHO_TABELA, CABECALHO_TABELA, 0, 10));
            sheet.createFreezePane(0, CABECALHO_TABELA + 1);
            int[] larguras = {10, 30, 18, 12, 18, 18, 18, 18, 12, 12, 16};
            for (int i = 0; i < larguras.length; i++) sheet.setColumnWidth(i, larguras[i] * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o Excel de clientes.", exception);
        }
    }

    private void criarCabecalho(
            Sheet sheet, Estilos e, String empresa, LocalDate inicio, LocalDate fim,
            LocalDateTime geradoEm, RelatorioClienteIndicadoresResponse i
    ) {
        mesclar(sheet, 0, empresa, e.titulo());
        mesclar(sheet, 1, "Relatório de Clientes", e.titulo());
        mesclar(sheet, 2, "Período: " + formatar(inicio) + " a " + formatar(fim), e.texto());
        mesclar(sheet, 3, "Gerado em: " + geradoEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), e.texto());
        String[] nomes = {"Clientes compradores", "Pedidos válidos", "Faturamento", "Ticket médio",
                "Clientes recorrentes", "Cliente líder", "Valor do líder"};
        Row rotulos = sheet.createRow(5);
        Row valores = sheet.createRow(6);
        for (int c = 0; c < nomes.length; c++) {
            rotulos.createCell(c).setCellValue(nomes[c]);
            rotulos.getCell(c).setCellStyle(e.negrito());
        }
        valores.createCell(0).setCellValue(i.clientesCompradores());
        valores.createCell(1).setCellValue(i.pedidosValidos());
        moeda(valores, 2, i.faturamentoTotal(), e.moeda());
        moeda(valores, 3, i.ticketMedioGeral(), e.moeda());
        valores.createCell(4).setCellValue(i.clientesRecorrentes());
        valores.createCell(5).setCellValue(i.clienteLiderNome());
        moeda(valores, 6, i.clienteLiderValor(), e.moeda());
    }

    private void criarTabela(Sheet sheet, Estilos e, List<RelatorioClienteLinhaResponse> linhas) {
        Row cabecalho = sheet.createRow(CABECALHO_TABELA);
        for (int c = 0; c < COLUNAS.length; c++) {
            cabecalho.createCell(c).setCellValue(COLUNAS[c]);
            cabecalho.getCell(c).setCellStyle(e.cabecalho());
        }
        int numeroLinha = CABECALHO_TABELA + 1;
        for (RelatorioClienteLinhaResponse linha : linhas) {
            Row row = sheet.createRow(numeroLinha++);
            row.createCell(0).setCellValue(linha.posicao());
            row.createCell(1).setCellValue(linha.clienteNome());
            row.createCell(2).setCellValue(formatacaoUtil.formatarTelefone(linha.clienteTelefone()));
            row.createCell(3).setCellValue(linha.pedidosValidos());
            moeda(row, 4, linha.faturamentoTotal(), e.moeda());
            moeda(row, 5, linha.ticketMedio(), e.moeda());
            data(row, 6, linha.primeiraCompra(), e.data());
            data(row, 7, linha.ultimaCompra(), e.data());
            row.createCell(8).setCellValue(linha.entregas());
            row.createCell(9).setCellValue(linha.retiradas());
            row.createCell(10).setCellValue(linha.participacaoPercentual().doubleValue() / 100);
            row.getCell(10).setCellStyle(e.percentual());
        }
    }

    private Estilos criarEstilos(Workbook workbook) {
        Font bold = workbook.createFont(); bold.setBold(true);
        CellStyle titulo = workbook.createCellStyle(); titulo.setFont(bold);
        CellStyle texto = workbook.createCellStyle();
        CellStyle negrito = workbook.createCellStyle(); negrito.setFont(bold);
        CellStyle cabecalho = workbook.createCellStyle(); cabecalho.setFont(bold);
        cabecalho.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        DataFormat formato = workbook.createDataFormat();
        CellStyle moeda = workbook.createCellStyle(); moeda.setDataFormat(formato.getFormat("R$ #,##0.00"));
        CellStyle percentual = workbook.createCellStyle(); percentual.setDataFormat(formato.getFormat("0.00%"));
        CellStyle data = workbook.createCellStyle(); data.setDataFormat(formato.getFormat("dd/mm/yyyy"));
        return new Estilos(titulo, texto, negrito, cabecalho, moeda, percentual, data);
    }

    private void mesclar(Sheet sheet, int linha, String valor, CellStyle estilo) {
        Row row = sheet.createRow(linha);
        row.createCell(0).setCellValue(valor);
        row.getCell(0).setCellStyle(estilo);
        sheet.addMergedRegion(new CellRangeAddress(linha, linha, 0, 10));
    }

    private void moeda(Row row, int coluna, java.math.BigDecimal valor, CellStyle estilo) {
        row.createCell(coluna).setCellValue(valor.doubleValue());
        row.getCell(coluna).setCellStyle(estilo);
    }

    private void data(Row row, int coluna, LocalDate valor, CellStyle estilo) {
        row.createCell(coluna).setCellValue(java.util.Date.from(
                valor.atStartOfDay(ZoneId.systemDefault()).toInstant()
        ));
        row.getCell(coluna).setCellStyle(estilo);
    }

    private String formatar(LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private record Estilos(CellStyle titulo, CellStyle texto, CellStyle negrito,
                           CellStyle cabecalho, CellStyle moeda, CellStyle percentual,
                           CellStyle data) {
    }
}
