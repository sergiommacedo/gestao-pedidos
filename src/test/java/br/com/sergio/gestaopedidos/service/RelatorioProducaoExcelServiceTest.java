package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.*;
import br.com.sergio.gestaopedidos.enums.UnidadeVenda;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelatorioProducaoExcelServiceTest {
    private final RelatorioProducaoExcelService service=new RelatorioProducaoExcelService();

    @Test void deveGerarXlsxRealComUnidadeQuiloMoedasEPercentuaisNumericos() throws Exception {
        var linhas=List.of(linha(1,UnidadeVenda.UNIDADE,"3","150","40"),linha(2,UnidadeVenda.QUILOGRAMA,"4.250","225","60"));
        byte[] bytes=service.gerar("Empresa",LocalDate.of(2026,8,1),LocalDate.of(2026,8,31),LocalDateTime.now(),linhas,indicadores());
        try(XSSFWorkbook w=new XSSFWorkbook(new ByteArrayInputStream(bytes))){var s=w.getSheet("Produção");assertThat(s).isNotNull();assertThat(s.getCTWorksheet().isSetAutoFilter()).isTrue();assertThat(s.getPaneInformation()).isNotNull();
            var unidade=s.getRow(12);var quilo=s.getRow(13);assertThat(unidade.getCell(3).getCellType()).isEqualTo(CellType.NUMERIC);assertThat(unidade.getCell(3).getCellStyle().getDataFormatString()).isEqualTo("0");
            assertThat(quilo.getCell(3).getNumericCellValue()).isEqualTo(4.25);assertThat(quilo.getCell(3).getCellStyle().getDataFormatString()).isEqualTo("0.000");
            assertThat(quilo.getCell(5).getCellType()).isEqualTo(CellType.NUMERIC);assertThat(quilo.getCell(5).getCellStyle().getDataFormatString()).contains("R$");
            assertThat(quilo.getCell(7).getCellType()).isEqualTo(CellType.NUMERIC);assertThat(quilo.getCell(7).getNumericCellValue()).isEqualTo(.6);assertThat(quilo.getCell(7).getCellStyle().getDataFormatString()).isEqualTo("0.00%");}
    }

    @Test void deveGerarXlsxVazioValido() throws Exception {byte[] b=service.gerar("Empresa",LocalDate.now(),LocalDate.now(),LocalDateTime.now(),List.of(),RelatorioProducaoIndicadoresResponse.vazio());try(XSSFWorkbook w=new XSSFWorkbook(new ByteArrayInputStream(b))){assertThat((Object)w.getSheet("Produção").getRow(11)).isNotNull();assertThat((Object)w.getSheet("Produção").getRow(12)).isNull();}}

    private RelatorioProducaoLinhaResponse linha(long id,UnidadeVenda u,String q,String f,String p){return new RelatorioProducaoLinhaResponse(id,"Produto "+id,u,new BigDecimal(q),2,new BigDecimal(f),new BigDecimal(f).divide(BigDecimal.valueOf(2)),new BigDecimal(p),id);}
    private RelatorioProducaoIndicadoresResponse indicadores(){return new RelatorioProducaoIndicadoresResponse(2,new BigDecimal("3"),new BigDecimal("4.250"),new BigDecimal("375"),new BigDecimal("35"),new BigDecimal("410"),"Produto 2",new BigDecimal("225"));}
}
