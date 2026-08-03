package br.com.sergio.gestaopedidos.service;

import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoIndicadoresResponse;
import br.com.sergio.gestaopedidos.dto.relatorio.RelatorioPedidoLinhaResponse;
import br.com.sergio.gestaopedidos.enums.StatusPedido;
import br.com.sergio.gestaopedidos.util.FormatacaoUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

@Service
public class RelatorioPedidoExcelService {

    private static final int LINHA_CABECALHO_TABELA = 11;
    private static final String[] COLUNAS = {
            "ID", "Data agendada", "Cadastro", "Cliente", "Telefone", "Status",
            "Entrega", "Pagamento", "Subtotal", "Taxa", "Total"
    };
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final FormatacaoUtil formatacaoUtil = new FormatacaoUtil();

    public byte[] gerar(
            String nomeEmpresa,
            LocalDate dataInicial,
            LocalDate dataFinal,
            LocalDateTime geradoEm,
            List<RelatorioPedidoLinhaResponse> pedidos,
            RelatorioPedidoIndicadoresResponse indicadores
    ) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Pedidos");
            Estilos estilos = criarEstilos(workbook);

            criarCabecalho(sheet, estilos, nomeEmpresa, dataInicial, dataFinal, geradoEm, indicadores);
            criarTabela(sheet, estilos, pedidos);
            ajustarPlanilha(sheet);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o arquivo Excel.", exception);
        }
    }

    private void criarCabecalho(
            Sheet sheet,
            Estilos estilos,
            String nomeEmpresa,
            LocalDate dataInicial,
            LocalDate dataFinal,
            LocalDateTime geradoEm,
            RelatorioPedidoIndicadoresResponse indicadores
    ) {
        criarCelulaMesclada(sheet, 0, nomeEmpresa, estilos.tituloEmpresa());
        criarCelulaMesclada(sheet, 1, "Relatório de Pedidos", estilos.tituloRelatorio());
        criarCelulaMesclada(
                sheet,
                2,
                "Período: " + DATA_BR.format(dataInicial) + " a " + DATA_BR.format(dataFinal),
                estilos.texto()
        );
        criarCelulaMesclada(
                sheet,
                3,
                "Gerado em: " + geradoEm.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                estilos.texto()
        );

        String[] rotulos = {"Pedidos", "Pedidos válidos", "Cancelados", "Faturamento", "Taxas", "Ticket médio"};
        Row linhaRotulos = sheet.createRow(5);
        for (int coluna = 0; coluna < rotulos.length; coluna++) {
            Cell celula = linhaRotulos.createCell(coluna);
            celula.setCellValue(rotulos[coluna]);
            celula.setCellStyle(estilos.rotuloIndicador());
        }
        Row valores = sheet.createRow(6);
        valores.createCell(0).setCellValue(indicadores.totalPedidos());
        valores.createCell(1).setCellValue(indicadores.pedidosValidos());
        valores.createCell(2).setCellValue(indicadores.cancelados());
        criarCelulaMonetaria(valores, 3, indicadores.faturamento().doubleValue(), estilos.moeda());
        criarCelulaMonetaria(valores, 4, indicadores.taxasEntrega().doubleValue(), estilos.moeda());
        criarCelulaMonetaria(valores, 5, indicadores.ticketMedio().doubleValue(), estilos.moeda());
    }

    private void criarTabela(Sheet sheet, Estilos estilos, List<RelatorioPedidoLinhaResponse> pedidos) {
        Row cabecalho = sheet.createRow(LINHA_CABECALHO_TABELA);
        for (int coluna = 0; coluna < COLUNAS.length; coluna++) {
            Cell celula = cabecalho.createCell(coluna);
            celula.setCellValue(COLUNAS[coluna]);
            celula.setCellStyle(estilos.cabecalhoTabela());
        }

        int numeroLinha = LINHA_CABECALHO_TABELA + 1;
        for (RelatorioPedidoLinhaResponse pedido : pedidos) {
            Row linha = sheet.createRow(numeroLinha++);
            linha.createCell(0).setCellValue(pedido.id());
            criarCelulaData(linha, 1, pedido.dataAgendada(), estilos.data());
            criarCelulaDataHora(linha, 2, pedido.dataPedido(), estilos.dataHora());
            linha.createCell(3).setCellValue(pedido.clienteNome());
            linha.createCell(4).setCellValue(formatacaoUtil.formatarTelefone(pedido.clienteTelefone()));
            linha.createCell(5).setCellValue(pedido.status().getDescricao());
            linha.createCell(6).setCellValue(pedido.tipoEntrega().name().equals("ENTREGA") ? "Entrega" : "Retirada");
            linha.createCell(7).setCellValue(pedido.formaPagamento().getDescricao());
            criarCelulaMonetaria(linha, 8, pedido.subtotal().doubleValue(), estilos.moeda());
            criarCelulaMonetaria(linha, 9, pedido.taxaEntrega().doubleValue(), estilos.moeda());
            criarCelulaMonetaria(linha, 10, pedido.valorTotal().doubleValue(), estilos.moeda());
            if (pedido.status() == StatusPedido.CANCELADO) {
                linha.getCell(0).setCellStyle(estilos.cancelado());
                linha.getCell(1).setCellStyle(estilos.dataCancelado());
                linha.getCell(2).setCellStyle(estilos.dataHoraCancelado());
                for (int coluna = 3; coluna <= 7; coluna++) {
                    linha.getCell(coluna).setCellStyle(estilos.cancelado());
                }
                for (int coluna = 8; coluna <= 10; coluna++) {
                    linha.getCell(coluna).setCellStyle(estilos.moedaCancelado());
                }
            }
        }
    }

    private void ajustarPlanilha(Sheet sheet) {
        sheet.setAutoFilter(new CellRangeAddress(
                LINHA_CABECALHO_TABELA,
                LINHA_CABECALHO_TABELA,
                0,
                COLUNAS.length - 1
        ));
        sheet.createFreezePane(0, LINHA_CABECALHO_TABELA + 1);
        int[] larguras = {10, 16, 20, 28, 18, 22, 14, 24, 16, 14, 16};
        for (int coluna = 0; coluna < larguras.length; coluna++) {
            sheet.setColumnWidth(coluna, larguras[coluna] * 256);
        }
    }

    private Estilos criarEstilos(Workbook workbook) {
        CellStyle tituloEmpresa = workbook.createCellStyle();
        Font fonteEmpresa = workbook.createFont();
        fonteEmpresa.setBold(true);
        fonteEmpresa.setFontHeightInPoints((short) 14);
        tituloEmpresa.setFont(fonteEmpresa);

        CellStyle tituloRelatorio = workbook.createCellStyle();
        Font fonteTitulo = workbook.createFont();
        fonteTitulo.setBold(true);
        fonteTitulo.setFontHeightInPoints((short) 16);
        tituloRelatorio.setFont(fonteTitulo);

        CellStyle texto = workbook.createCellStyle();
        CellStyle rotulo = workbook.createCellStyle();
        Font fonteNegrito = workbook.createFont();
        fonteNegrito.setBold(true);
        rotulo.setFont(fonteNegrito);

        CellStyle cabecalho = workbook.createCellStyle();
        cabecalho.setFont(fonteNegrito);
        cabecalho.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        cabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cabecalho.setAlignment(HorizontalAlignment.CENTER);

        CellStyle moeda = workbook.createCellStyle();
        moeda.setDataFormat(workbook.createDataFormat().getFormat("R$ #,##0.00"));
        CellStyle data = workbook.createCellStyle();
        data.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy"));
        CellStyle dataHora = workbook.createCellStyle();
        dataHora.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

        CellStyle cancelado = criarEstiloCancelado(workbook, null);
        CellStyle moedaCancelado = criarEstiloCancelado(workbook, moeda);
        CellStyle dataCancelado = criarEstiloCancelado(workbook, data);
        CellStyle dataHoraCancelado = criarEstiloCancelado(workbook, dataHora);

        return new Estilos(
                tituloEmpresa,
                tituloRelatorio,
                texto,
                rotulo,
                cabecalho,
                moeda,
                data,
                dataHora,
                cancelado,
                moedaCancelado,
                dataCancelado,
                dataHoraCancelado
        );
    }

    private CellStyle criarEstiloCancelado(Workbook workbook, CellStyle base) {
        CellStyle estilo = workbook.createCellStyle();
        if (base != null) {
            estilo.cloneStyleFrom(base);
        }
        estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private void criarCelulaMesclada(Sheet sheet, int linha, String valor, CellStyle estilo) {
        Row row = sheet.createRow(linha);
        Cell celula = row.createCell(0);
        celula.setCellValue(valor);
        celula.setCellStyle(estilo);
        sheet.addMergedRegion(new CellRangeAddress(linha, linha, 0, COLUNAS.length - 1));
    }

    private void criarCelulaMonetaria(Row linha, int coluna, double valor, CellStyle estilo) {
        Cell celula = linha.createCell(coluna);
        celula.setCellValue(valor);
        celula.setCellStyle(estilo);
    }

    private void criarCelulaData(Row linha, int coluna, LocalDate valor, CellStyle estilo) {
        Cell celula = linha.createCell(coluna);
        celula.setCellValue(Date.from(valor.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        celula.setCellStyle(estilo);
    }

    private void criarCelulaDataHora(Row linha, int coluna, LocalDateTime valor, CellStyle estilo) {
        Cell celula = linha.createCell(coluna);
        celula.setCellValue(Date.from(valor.atZone(ZoneId.systemDefault()).toInstant()));
        celula.setCellStyle(estilo);
    }

    private record Estilos(
            CellStyle tituloEmpresa,
            CellStyle tituloRelatorio,
            CellStyle texto,
            CellStyle rotuloIndicador,
            CellStyle cabecalhoTabela,
            CellStyle moeda,
            CellStyle data,
            CellStyle dataHora,
            CellStyle cancelado,
            CellStyle moedaCancelado,
            CellStyle dataCancelado,
            CellStyle dataHoraCancelado
    ) {
    }
}
