package br.com.dashboard.exportation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/export")
public class ExportController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            );

    private final ExcelExportService excelExportService;

    public ExportController(
            ExcelExportService excelExportService
    ) {
        this.excelExportService =
                excelExportService;
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel() {

        byte[] file =
                excelExportService.exportDemands();

        String fileName =
                "dashboard-demandas-" +
                        LocalDate.now() +
                        ".xlsx";

        return ResponseEntity
                .ok()
                .contentType(
                        XLSX_MEDIA_TYPE
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                fileName +
                                "\""
                )
                .contentLength(
                        file.length
                )
                .body(file);
    }
}