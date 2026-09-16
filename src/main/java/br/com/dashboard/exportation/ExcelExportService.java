package br.com.dashboard.exportation;

import br.com.dashboard.demand.Demand;
import br.com.dashboard.demand.DemandService;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final DateTimeFormatter TITLE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM");

    private static final String[] HEADERS = {
            "CÓDIGO",
            "SERVIÇO",
            "ETAPA",
            "RESPONSÁVEL ATUAL",
            "CADASTRO",
            "QUALIFICAÇÃO",
            "VENCIMENTO",
            "REINGRESSO"
    };

    private final DemandService demandService;

    public ExcelExportService(DemandService demandService) {
        this.demandService = demandService;
    }

    public byte[] exportDemands() {

        List<Demand> demands =
                demandService.findAll();

        List<Demand> initialDemands =
                demands.stream()
                        .filter(this::isInitialConference)
                        .sorted(qualificationComparator())
                        .toList();

        List<Demand> finalDemands =
                demands.stream()
                        .filter(this::isFinalConference)
                        .sorted(qualificationComparator())
                        .toList();

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            CellStyle titleStyle =
                    createTitleStyle(workbook);

            CellStyle headerStyle =
                    createHeaderStyle(workbook);

            CellStyle regularStyle =
                    createRegularStyle(workbook);

            CellStyle overdueStyle =
                    createOverdueStyle(workbook);

            CellStyle todayStyle =
                    createTodayStyle(workbook);

            CellStyle upcomingStyle =
                    createUpcomingStyle(workbook);

            CellStyle completedStyle =
                    createCompletedStyle(workbook);

            CellStyle totalStyle =
                    createTotalStyle(workbook);

            createSheet(
                    workbook,
                    "Conferência Inicial",
                    initialDemands,
                    titleStyle,
                    headerStyle,
                    regularStyle,
                    overdueStyle,
                    todayStyle,
                    upcomingStyle,
                    completedStyle,
                    totalStyle
            );

            createSheet(
                    workbook,
                    "Conferência Final",
                    finalDemands,
                    titleStyle,
                    headerStyle,
                    regularStyle,
                    overdueStyle,
                    todayStyle,
                    upcomingStyle,
                    completedStyle,
                    totalStyle
            );

            workbook.write(output);

            return output.toByteArray();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Não foi possível gerar o arquivo Excel.",
                    exception
            );
        }
    }

    private void createSheet(
            Workbook workbook,
            String sheetName,
            List<Demand> demands,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle regularStyle,
            CellStyle overdueStyle,
            CellStyle todayStyle,
            CellStyle upcomingStyle,
            CellStyle completedStyle,
            CellStyle totalStyle
    ) {

        Sheet sheet =
                workbook.createSheet(sheetName);

        createTitle(
                sheet,
                titleStyle
        );

        createHeader(
                sheet,
                headerStyle
        );

        int rowIndex = 2;

        for (Demand demand : demands) {

            Row row =
                    sheet.createRow(rowIndex++);

            row.setHeightInPoints(22);

            CellStyle rowStyle =
                    resolveRowStyle(
                            demand,
                            regularStyle,
                            overdueStyle,
                            todayStyle,
                            upcomingStyle,
                            completedStyle
                    );

            createCell(
                    row,
                    0,
                    demand.getExternalId(),
                    rowStyle
            );

            createCell(
                    row,
                    1,
                    demand.getType(),
                    rowStyle
            );

            createCell(
                    row,
                    2,
                    demand.getStage(),
                    rowStyle
            );

            createCell(
                    row,
                    3,
                    demand.getResponsible(),
                    rowStyle
            );

            createCell(
                    row,
                    4,
                    formatDate(
                            demand.getEntryDate()
                    ),
                    rowStyle
            );

            createCell(
                    row,
                    5,
                    formatDate(
                            demand.getQualificationDate()
                    ),
                    rowStyle
            );

            createCell(
                    row,
                    6,
                    formatDate(
                            demand.getDeadline()
                    ),
                    rowStyle
            );

            createCell(
                    row,
                    7,
                    formatDate(
                            demand.getReentryDate()
                    ),
                    rowStyle
            );
        }

        createTotal(
                sheet,
                rowIndex + 1,
                demands.size(),
                totalStyle
        );

        configureSheet(
                sheet,
                demands.size()
        );
    }

    private void createTitle(
            Sheet sheet,
            CellStyle titleStyle
    ) {

        Row titleRow =
                sheet.createRow(0);

        titleRow.setHeightInPoints(24);

        sheet.addMergedRegion(
                new CellRangeAddress(
                        0,
                        0,
                        0,
                        HEADERS.length - 1
                )
        );

        Cell cell =
                titleRow.createCell(0);

        String title =
                "Situação em " +
                        LocalDate.now().format(
                                TITLE_DATE_FORMATTER
                        );

        cell.setCellValue(title);
        cell.setCellStyle(titleStyle);

        for (
                int column = 1;
                column < HEADERS.length;
                column++
        ) {

            Cell mergedCell =
                    titleRow.createCell(column);

            mergedCell.setCellStyle(
                    titleStyle
            );
        }
    }

    private void createHeader(
            Sheet sheet,
            CellStyle headerStyle
    ) {

        Row headerRow =
                sheet.createRow(1);

        headerRow.setHeightInPoints(24);

        for (
                int column = 0;
                column < HEADERS.length;
                column++
        ) {

            Cell cell =
                    headerRow.createCell(column);

            cell.setCellValue(
                    HEADERS[column]
            );

            cell.setCellStyle(
                    headerStyle
            );
        }
    }

    private void createTotal(
            Sheet sheet,
            int rowIndex,
            int total,
            CellStyle totalStyle
    ) {

        Row totalRow =
                sheet.createRow(rowIndex);

        sheet.addMergedRegion(
                new CellRangeAddress(
                        rowIndex,
                        rowIndex,
                        6,
                        7
                )
        );

        Cell totalCell =
                totalRow.createCell(6);

        totalCell.setCellValue(
                "TOTAL " + total
        );

        totalCell.setCellStyle(
                totalStyle
        );

        Cell secondCell =
                totalRow.createCell(7);

        secondCell.setCellStyle(
                totalStyle
        );
    }

    private void configureSheet(
            Sheet sheet,
            int totalRows
    ) {

        sheet.createFreezePane(
                0,
                2
        );

        if (totalRows > 0) {

            sheet.setAutoFilter(
                    new CellRangeAddress(
                            1,
                            totalRows + 1,
                            0,
                            HEADERS.length - 1
                    )
            );
        }

        sheet.setColumnWidth(
                0,
                14 * 256
        );

        sheet.setColumnWidth(
                1,
                32 * 256
        );

        sheet.setColumnWidth(
                2,
                30 * 256
        );

        sheet.setColumnWidth(
                3,
                30 * 256
        );

        sheet.setColumnWidth(
                4,
                15 * 256
        );

        sheet.setColumnWidth(
                5,
                15 * 256
        );

        sheet.setColumnWidth(
                6,
                15 * 256
        );

        sheet.setColumnWidth(
                7,
                15 * 256
        );
    }

    private boolean isInitialConference(
            Demand demand
    ) {

        String sector =
                normalizeText(
                        demand.getSector()
                );

        String stage =
                normalizeText(
                        demand.getStage()
                );

        return (
                sector.contains(
                        "conferencia inicial"
                ) ||
                        sector.contains(
                                "conferencia_inicial"
                        ) ||
                        stage.contains(
                                "conferencia inicial"
                        )
        );
    }

    private boolean isFinalConference(
            Demand demand
    ) {

        String sector =
                normalizeText(
                        demand.getSector()
                );

        String stage =
                normalizeText(
                        demand.getStage()
                );

        return (
                sector.contains(
                        "conferencia final"
                ) ||
                        sector.contains(
                                "conferencia_final"
                        ) ||
                        stage.contains(
                                "conferencia final"
                        )
        );
    }

    private String normalizeText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        String normalized =
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                );

        return normalized
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private Comparator<Demand>
    qualificationComparator() {

        return Comparator.comparing(
                Demand::getQualificationDate,
                Comparator.nullsLast(
                        Comparator.naturalOrder()
                )
        );
    }

    private CellStyle resolveRowStyle(
            Demand demand,
            CellStyle regularStyle,
            CellStyle overdueStyle,
            CellStyle todayStyle,
            CellStyle upcomingStyle,
            CellStyle completedStyle
    ) {

        if (isCompleted(demand)) {
            return completedStyle;
        }

        LocalDate qualificationDate =
                demand.getQualificationDate();

        if (qualificationDate == null) {
            return regularStyle;
        }

        LocalDate today =
                LocalDate.now();

        if (
                qualificationDate.isBefore(today)
        ) {
            return overdueStyle;
        }

        if (
                qualificationDate.isEqual(today)
        ) {
            return todayStyle;
        }

        if (
                !qualificationDate.isAfter(
                        today.plusDays(5)
                )
        ) {
            return upcomingStyle;
        }

        return regularStyle;
    }

    private boolean isCompleted(
            Demand demand
    ) {

        String status =
                normalizeText(
                        demand.getStatus()
                );

        return status.equals(
                "concluida"
        );
    }

    private CellStyle createTitleStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);
        font.setFontHeightInPoints(
                (short) 12
        );

        style.setFont(font);

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setFillForegroundColor(
                IndexedColors.LIGHT_TURQUOISE.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        applyBorders(style);

        return style;
    }

    private CellStyle createHeaderStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);

        font.setColor(
                IndexedColors.WHITE.getIndex()
        );

        style.setFont(font);

        style.setFillForegroundColor(
                IndexedColors.DARK_BLUE.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setWrapText(true);

        applyBorders(style);

        return style;
    }

    private CellStyle createRegularStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        configureDataStyle(style);

        return style;
    }

    private CellStyle createOverdueStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.ROSE.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        configureDataStyle(style);

        return style;
    }

    private CellStyle createTodayStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.LIGHT_ORANGE.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        configureDataStyle(style);

        return style;
    }

    private CellStyle createUpcomingStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.LIGHT_YELLOW.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        configureDataStyle(style);

        return style;
    }

    private CellStyle createCompletedStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.LIGHT_GREEN.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        configureDataStyle(style);

        return style;
    }

    private CellStyle createTotalStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);

        style.setFont(font);

        style.setFillForegroundColor(
                IndexedColors.YELLOW.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        applyBorders(style);

        return style;
    }

    private void configureDataStyle(
            CellStyle style
    ) {

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setWrapText(true);

        applyBorders(style);
    }

    private void applyBorders(
            CellStyle style
    ) {

        style.setBorderTop(
                BorderStyle.THIN
        );

        style.setBorderBottom(
                BorderStyle.THIN
        );

        style.setBorderLeft(
                BorderStyle.THIN
        );

        style.setBorderRight(
                BorderStyle.THIN
        );
    }

    private void createCell(
            Row row,
            int column,
            String value,
            CellStyle style
    ) {

        Cell cell =
                row.createCell(column);

        cell.setCellValue(
                value == null
                        ? ""
                        : value
        );

        cell.setCellStyle(style);
    }

    private String formatDate(
            LocalDate date
    ) {

        if (date == null) {
            return "";
        }

        return date.format(
                DATE_FORMATTER
        );
    }
}