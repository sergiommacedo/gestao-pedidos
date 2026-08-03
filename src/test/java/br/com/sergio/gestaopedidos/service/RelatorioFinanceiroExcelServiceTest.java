package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.*;
import br.com.sergio.gestaopedidos.enums.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class RelatorioFinanceiroExcelServiceTest {
    private final RelatorioFinanceiroExcelService service=new RelatorioFinanceiroExcelService();

    @Test void deveGerarXlsxRealComDuasAbasDatasMoedasEPercentuaisNumericos() throws Exception {
        var i=new RelatorioFinanceiroIndicadoresResponse(3,2,1,bd("180"),bd("20"),bd("200"),bd("100"),bd("50"),LocalDate.of(2026,8,2),bd("200"));
        var p=List.of(new RelatorioFinanceiroPagamentoResponse(FormaPagamento.PIX,2L,bd("200"),bd("100"),bd("100")));
        var e=List.of(new RelatorioFinanceiroEntregaResponse(TipoEntrega.ENTREGA,2L,bd("180"),bd("20"),bd("200"),bd("100")));
        var d=List.of(new RelatorioFinanceiroDiaResponse(LocalDate.of(2026,8,2),2L,1L,bd("180"),bd("20"),bd("200"),bd("100")));
        byte[] arquivo=service.gerar("Empresa",LocalDate.of(2026,8,1),LocalDate.of(2026,8,31),LocalDateTime.now(),i,p,e,d);
        try(XSSFWorkbook w=new XSSFWorkbook(new ByteArrayInputStream(arquivo))){assertThat(w.getNumberOfSheets()).isEqualTo(2);assertThat(w.getSheet("Resumo")).isNotNull();var s=w.getSheet("Faturamento diário");assertThat(s).isNotNull();assertThat(s.getCTWorksheet().isSetAutoFilter()).isTrue();assertThat(s.getPaneInformation()).isNotNull();var row=s.getRow(1);assertThat(row.getCell(0).getCellType()).isEqualTo(CellType.NUMERIC);assertThat(row.getCell(0).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy");assertThat(row.getCell(3).getCellType()).isEqualTo(CellType.NUMERIC);assertThat(row.getCell(3).getCellStyle().getDataFormatString()).contains("R$");var resumo=w.getSheet("Resumo");assertThat(resumo.getRow(13).getCell(3).getCellType()).isEqualTo(CellType.NUMERIC);assertThat(resumo.getRow(13).getCell(3).getCellStyle().getDataFormatString()).isEqualTo("0.00%");}
    }

    @Test void deveGerarEstruturaValidaSemMovimento() throws Exception {byte[] a=service.gerar("Empresa",LocalDate.now(),LocalDate.now(),LocalDateTime.now(),RelatorioFinanceiroIndicadoresResponse.vazio(),List.of(),List.of(),List.of());try(XSSFWorkbook w=new XSSFWorkbook(new ByteArrayInputStream(a))){assertThat(w.getNumberOfSheets()).isEqualTo(2);assertThat((Object)w.getSheet("Faturamento diário").getRow(1)).isNull();}}
    private BigDecimal bd(String v){return new BigDecimal(v);}
}
