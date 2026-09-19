package br.com.dashboard.exportation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/export")
public class ExportController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

    private static final DateTimeFormatter FILE_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(
                    "yyyy-MM-dd-HHmm"
            );

    private final ExcelExportService excelExportService;

    public ExportController(
            ExcelExportService excelExportService
    ) {
        this.excelExportService =
                excelExportService;
    }

    /**
     * Exporta a situação atual ou uma
     * fotografia histórica específica.
     *
     * Exemplos:
     *
     * Situação atual:
     * /export/excel
     *
     * Fotografia histórica:
     * /export/excel?referenceDateTime=2026-09-18T14:00
     */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel(

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime referenceDateTime

    ) {

        byte[] file =
                excelExportService.exportDemands(
                        referenceDateTime
                );

        String fileName;

        if (referenceDateTime == null) {

            fileName =
                    "dashboard-demandas-"
                            + LocalDate.now()
                            + ".xlsx";

        } else {

            fileName =
                    "dashboard-demandas-"
                            + referenceDateTime.format(
                            FILE_DATE_TIME_FORMATTER
                    )
                            + ".xlsx";
        }

        return ResponseEntity
                .ok()
                .contentType(
                        XLSX_MEDIA_TYPE
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + fileName
                                + "\""
                )
                .contentLength(
                        file.length
                )
                .body(
                        file
                );
    }
}