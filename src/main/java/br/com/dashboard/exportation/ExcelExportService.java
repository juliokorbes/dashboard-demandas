package br.com.dashboard.exportation;

import br.com.dashboard.demand.Demand;
import br.com.dashboard.demand.DemandService;
import br.com.dashboard.history.DemandSnapshot;
import br.com.dashboard.history.DemandSnapshotService;
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
import java.time.LocalDateTime;
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

    private static final DateTimeFormatter TITLE_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM - HH:mm");

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
    private final DemandSnapshotService demandSnapshotService;

    public ExcelExportService(
            DemandService demandService,
            DemandSnapshotService demandSnapshotService
    ) {
        this.demandService =
                demandService;

        this.demandSnapshotService =
                demandSnapshotService;
    }

    /**
     * Mantém compatibilidade com a exportação
     * da situação atual.
     */
    public byte[] exportDemands() {
        return exportDemands(null);
    }

    /**
     * Exporta a situação atual ou uma fotografia
     * histórica específica.
     *
     * Se referenceDateTime for null:
     * exporta a situação atual.
     *
     * Se referenceDateTime for informado:
     * exporta exatamente aquela fotografia.
     */
    public byte[] exportDemands(
            LocalDateTime referenceDateTime
    ) {

        List<ExportRow> demands;

        LocalDate comparisonDate;

        String title;

        if (referenceDateTime == null) {

            demands =
                    demandService
                            .findAll()
                            .stream()
                            .map(this::toExportRow)
                            .toList();

            comparisonDate =
                    LocalDate.now();

            title =
                    "Situação em "
                            + comparisonDate.format(
                            TITLE_DATE_FORMATTER
                    );

        } else {

            demands =
                    demandSnapshotService
                            .findByReferenceDateTime(
                                    referenceDateTime
                            )
                            .stream()
                            .map(this::toExportRow)
                            .toList();

            comparisonDate =
                    referenceDateTime.toLocalDate();

            title =
                    "Situação em "
                            + referenceDateTime.format(
                            TITLE_DATE_TIME_FORMATTER
                    );
        }

        List<ExportRow> initialDemands =
                demands
                        .stream()
                        .filter(
                                this::isInitialConference
                        )
                        .sorted(
                                qualificationComparator()
                        )
                        .toList();

        List<ExportRow> finalDemands =
                demands
                        .stream()
                        .filter(
                                this::isFinalConference
                        )
                        .sorted(
                                qualificationComparator()
                        )
                        .toList();

        try (
                Workbook workbook =
                        new XSSFWorkbook();

                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            CellStyle titleStyle =
                    createTitleStyle(
                            workbook
                    );

            CellStyle headerStyle =
                    createHeaderStyle(
                            workbook
                    );

            CellStyle regularStyle =
                    createRegularStyle(
                            workbook
                    );

            CellStyle overdueStyle =
                    createOverdueStyle(
                            workbook
                    );

            CellStyle todayStyle =
                    createTodayStyle(
                            workbook
                    );

            CellStyle upcomingStyle =
                    createUpcomingStyle(
                            workbook
                    );

            CellStyle completedStyle =
                    createCompletedStyle(
                            workbook
                    );

            CellStyle totalStyle =
                    createTotalStyle(
                            workbook
                    );

            createSheet(
                    workbook,
                    "Conferência Inicial",
                    initialDemands,
                    title,
                    comparisonDate,
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
                    title,
                    comparisonDate,
                    titleStyle,
                    headerStyle,
                    regularStyle,
                    overdueStyle,
                    todayStyle,
                    upcomingStyle,
                    completedStyle,
                    totalStyle
            );

            workbook.write(
                    output
            );

            return output
                    .toByteArray();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Não foi possível gerar o arquivo Excel.",
                    exception
            );
        }
    }

    /**
     * Converte uma demanda atual para o formato
     * interno utilizado pela exportação.
     */
    private ExportRow toExportRow(
            Demand demand
    ) {

        return new ExportRow(
                demand.getExternalId(),
                demand.getType(),
                demand.getStage(),
                demand.getSector(),
                demand.getResponsible(),
                demand.getEntryDate(),
                demand.getQualificationDate(),
                demand.getDeadline(),
                demand.getReentryDate(),
                demand.getStatus()
        );
    }

    /**
     * Converte uma fotografia histórica para
     * o mesmo formato utilizado na exportação.
     */
    private ExportRow toExportRow(
            DemandSnapshot snapshot
    ) {

        return new ExportRow(
                snapshot.getExternalId(),
                snapshot.getType(),
                snapshot.getStage(),
                snapshot.getSector(),
                snapshot.getResponsible(),
                snapshot.getEntryDate(),
                snapshot.getQualificationDate(),
                snapshot.getDeadline(),
                snapshot.getReentryDate(),
                snapshot.getStatus()
        );
    }

    private void createSheet(
            Workbook workbook,
            String sheetName,
            List<ExportRow> demands,
            String title,
            LocalDate comparisonDate,
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
                workbook.createSheet(
                        sheetName
                );

        createTitle(
                sheet,
                title,
                titleStyle
        );

        createHeader(
                sheet,
                headerStyle
        );

        int rowIndex = 2;

        for (ExportRow demand : demands) {

            Row row =
                    sheet.createRow(
                            rowIndex++
                    );

            row.setHeightInPoints(
                    22
            );

            CellStyle rowStyle =
                    resolveRowStyle(
                            demand,
                            comparisonDate,
                            regularStyle,
                            overdueStyle,
                            todayStyle,
                            upcomingStyle,
                            completedStyle
                    );

            createCell(
                    row,
                    0,
                    demand.externalId(),
                    rowStyle
            );

            createCell(
                    row,
                    1,
                    demand.type(),
                    rowStyle
            );

            createCell(
                    row,
                    2,
                    demand.stage(),
                    rowStyle
            );

            createCell(
                    row,
                    3,
                    demand.responsible(),
                    rowStyle
            );

            createCell(
                    row,
                    4,
                    formatDate(
                            demand.entryDate()
                    ),
                    rowStyle
            );

            createCell(
                    row,
                    5,
                    formatDate(
                            demand.qualificationDate()
                    ),
                    rowStyle
            );

            createCell(
                    row,
                    6,
                    formatDate(
                            demand.deadline()
                    ),
                    rowStyle
            );

            createCell(
                    row,
                    7,
                    formatDate(
                            demand.reentryDate()
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
            String title,
            CellStyle titleStyle
    ) {

        Row titleRow =
                sheet.createRow(0);

        titleRow.setHeightInPoints(
                24
        );

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

        cell.setCellValue(
                title
        );

        cell.setCellStyle(
                titleStyle
        );

        for (
                int column = 1;
                column < HEADERS.length;
                column++
        ) {

            Cell mergedCell =
                    titleRow.createCell(
                            column
                    );

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

        headerRow.setHeightInPoints(
                24
        );

        for (
                int column = 0;
                column < HEADERS.length;
                column++
        ) {

            Cell cell =
                    headerRow.createCell(
                            column
                    );

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
                sheet.createRow(
                        rowIndex
                );

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
            ExportRow demand
    ) {

        String sector =
                normalizeText(
                        demand.sector()
                );

        String stage =
                normalizeText(
                        demand.stage()
                );

        return (
                sector.contains(
                        "conferencia inicial"
                )
                        ||
                        sector.contains(
                                "conferencia_inicial"
                        )
                        ||
                        stage.contains(
                                "conferencia inicial"
                        )
        );
    }

    private boolean isFinalConference(
            ExportRow demand
    ) {

        String sector =
                normalizeText(
                        demand.sector()
                );

        String stage =
                normalizeText(
                        demand.stage()
                );

        return (
                sector.contains(
                        "conferencia final"
                )
                        ||
                        sector.contains(
                                "conferencia_final"
                        )
                        ||
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

    private Comparator<ExportRow>
    qualificationComparator() {

        return Comparator.comparing(
                ExportRow::qualificationDate,
                Comparator.nullsLast(
                        Comparator.naturalOrder()
                )
        );
    }

    private CellStyle resolveRowStyle(
            ExportRow demand,
            LocalDate comparisonDate,
            CellStyle regularStyle,
            CellStyle overdueStyle,
            CellStyle todayStyle,
            CellStyle upcomingStyle,
            CellStyle completedStyle
    ) {

        /*
         * Só utiliza o verde se o próprio
         * relatório trouxer explicitamente
         * status de conclusão.
         *
         * Desaparecer da fotografia seguinte
         * NÃO significa conclusão.
         */
        if (isCompleted(demand)) {
            return completedStyle;
        }

        LocalDate qualificationDate =
                demand.qualificationDate();

        if (qualificationDate == null) {
            return regularStyle;
        }

        if (
                qualificationDate.isBefore(
                        comparisonDate
                )
        ) {
            return overdueStyle;
        }

        if (
                qualificationDate.isEqual(
                        comparisonDate
                )
        ) {
            return todayStyle;
        }

        if (
                !qualificationDate.isAfter(
                        comparisonDate.plusDays(5)
                )
        ) {
            return upcomingStyle;
        }

        return regularStyle;
    }

    private boolean isCompleted(
            ExportRow demand
    ) {

        String status =
                normalizeText(
                        demand.status()
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

        style.setFont(
                font
        );

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setFillForegroundColor(
                IndexedColors.LIGHT_TURQUOISE
                        .getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        applyBorders(
                style
        );

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

        style.setFont(
                font
        );

        style.setFillForegroundColor(
                IndexedColors.DARK_BLUE
                        .getIndex()
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

        style.setWrapText(
                true
        );

        applyBorders(
                style
        );

        return style;
    }

    private CellStyle createRegularStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        configureDataStyle(
                style
        );

        return style;
    }

    private CellStyle createOverdueStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.ROSE
                        .getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        configureDataStyle(
                style
        );

        return style;
    }

    private CellStyle createTodayStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.LIGHT_ORANGE
                        .getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        configureDataStyle(
                style
        );

        return style;
    }

    private CellStyle createUpcomingStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.LIGHT_YELLOW
                        .getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        configureDataStyle(
                style
        );

        return style;
    }

    private CellStyle createCompletedStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setFillForegroundColor(
                IndexedColors.LIGHT_GREEN
                        .getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        configureDataStyle(
                style
        );

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

        style.setFont(
                font
        );

        style.setFillForegroundColor(
                IndexedColors.YELLOW
                        .getIndex()
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

        applyBorders(
                style
        );

        return style;
    }

    private void configureDataStyle(
            CellStyle style
    ) {

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setWrapText(
                true
        );

        applyBorders(
                style
        );
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
                row.createCell(
                        column
                );

        cell.setCellValue(
                value == null
                        ? ""
                        : value
        );

        cell.setCellStyle(
                style
        );
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

    /**
     * Estrutura comum usada tanto para a
     * situação atual quanto para snapshots.
     */
    private record ExportRow(
            String externalId,
            String type,
            String stage,
            String sector,
            String responsible,
            LocalDate entryDate,
            LocalDate qualificationDate,
            LocalDate deadline,
            LocalDate reentryDate,
            String status
    ) {
    }
}