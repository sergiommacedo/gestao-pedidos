package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioClienteIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioClienteLinhaResponse;
import br.com.sergio.gestaopedidos.util.FormatacaoUtil;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioClienteExcelServiceTest {
    private final RelatorioClienteExcelService service = new RelatorioClienteExcelService(new FormatacaoUtil());

    @Test
    void deveGerarXlsxRealComTelefoneDatasMoedasEPercentuaisNumericos() throws Exception {
        var linha = new RelatorioClienteLinhaResponse(
                1L, "Maria", "11999990000", 2L, new BigDecimal("250.50"),
                new BigDecimal("125.25"), LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 20),
                1L, 1L, new BigDecimal("62.50"), 1L
        );
        var indicadores = new RelatorioClienteIndicadoresResponse(
                1, 2, new BigDecimal("250.50"), new BigDecimal("125.25"),
                1, "Maria", new BigDecimal("250.50")
        );
        byte[] arquivo = service.gerar(
                "Empresa", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31),
                LocalDateTime.of(2026, 8, 31, 10, 30), List.of(linha), indicadores
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            var sheet = workbook.getSheet("Clientes");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getCTWorksheet().isSetAutoFilter()).isTrue();
            assertThat(sheet.getPaneInformation()).isNotNull();
            var row = sheet.getRow(12);
            assertThat(row.getCell(2).getStringCellValue()).isEqualTo("(11) 99999-0000");
            assertThat(row.getCell(4).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(row.getCell(4).getCellStyle().getDataFormatString()).contains("R$");
            assertThat(row.getCell(6).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(row.getCell(6).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy");
            assertThat(row.getCell(10).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(row.getCell(10).getNumericCellValue()).isEqualTo(.625);
            assertThat(row.getCell(10).getCellStyle().getDataFormatString()).isEqualTo("0.00%");
        }
    }

    @Test
    void deveGerarXlsxVazioValido() throws Exception {
        byte[] arquivo = service.gerar(
                "Empresa", LocalDate.now(), LocalDate.now(), LocalDateTime.now(),
                List.of(), RelatorioClienteIndicadoresResponse.vazio()
        );
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            assertThat((Object) workbook.getSheet("Clientes").getRow(11)).isNotNull();
            assertThat((Object) workbook.getSheet("Clientes").getRow(12)).isNull();
        }
    }
}
