package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoLinhaResponse;
import br.com.sergio.gestaopedidos.enums.FormaPagamento;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.enums.TipoEntrega;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioPedidoExcelServiceTest {

    private final RelatorioPedidoExcelService service = new RelatorioPedidoExcelService();

    @Test
    void deveGerarXlsxValidoComDatasMoedasNumericasEFiltrosDaTabela() throws Exception {
        byte[] arquivo = service.gerar(
                "Empresa Teste",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                LocalDateTime.of(2026, 8, 31, 18, 30),
                List.of(pedido(StatusPedido.ENTREGUE)),
                indicadores(1, 1, 0, "123.45", "10.00", "123.45")
        );

        assertThat(arquivo).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            var sheet = workbook.getSheet("Pedidos");
            assertThat(sheet).isNotNull();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Empresa Teste");
            assertThat(sheet.getRow(11).getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(sheet.getCTWorksheet().isSetAutoFilter()).isTrue();
            assertThat(sheet.getPaneInformation()).isNotNull();

            var linha = sheet.getRow(12);
            assertThat(linha.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(DateUtil.isCellDateFormatted(linha.getCell(1))).isTrue();
            assertThat(linha.getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(DateUtil.isCellDateFormatted(linha.getCell(2))).isTrue();
            assertThat(linha.getCell(8).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(linha.getCell(8).getNumericCellValue()).isEqualTo(100.0);
            assertThat(linha.getCell(9).getNumericCellValue()).isEqualTo(10.0);
            assertThat(linha.getCell(10).getNumericCellValue()).isEqualTo(110.0);
            assertThat(linha.getCell(10).getCellStyle().getDataFormatString()).contains("R$");
        }
    }

    @Test
    void deveGerarXlsxValidoSemRegistros() throws Exception {
        byte[] arquivo = service.gerar(
                "Empresa Teste",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                LocalDateTime.of(2026, 8, 31, 18, 30),
                List.of(),
                RelatorioPedidoIndicadoresResponse.vazio()
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            assertThat((Object) workbook.getSheet("Pedidos").getRow(11)).isNotNull();
            assertThat((Object) workbook.getSheet("Pedidos").getRow(12)).isNull();
        }
    }

    @Test
    void deveDestacarCanceladoSemConverterValoresEmTexto() throws Exception {
        byte[] arquivo = service.gerar(
                "Empresa Teste",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31),
                LocalDateTime.of(2026, 8, 31, 18, 30),
                List.of(pedido(StatusPedido.CANCELADO)),
                indicadores(1, 0, 1, "0", "0", "0")
        );

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo))) {
            var linha = workbook.getSheet("Pedidos").getRow(12);
            assertThat(linha.getCell(5).getStringCellValue()).isEqualTo("Cancelado");
            assertThat(linha.getCell(10).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(linha.getCell(5).getCellStyle().getFillPattern().name()).isEqualTo("SOLID_FOREGROUND");
        }
    }

    private RelatorioPedidoLinhaResponse pedido(StatusPedido status) {
        return new RelatorioPedidoLinhaResponse(
                15L,
                LocalDate.of(2026, 8, 10),
                LocalDateTime.of(2026, 8, 5, 14, 20),
                "Maria da Silva",
                "41999999999",
                status,
                TipoEntrega.ENTREGA,
                FormaPagamento.PIX,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("110.00")
        );
    }

    private RelatorioPedidoIndicadoresResponse indicadores(
            long total,
            long validos,
            long cancelados,
            String faturamento,
            String taxas,
            String ticket
    ) {
        return new RelatorioPedidoIndicadoresResponse(
                total,
                validos,
                cancelados,
                new BigDecimal(faturamento),
                new BigDecimal(taxas),
                new BigDecimal(ticket)
        );
    }
}
