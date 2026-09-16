package br.com.dashboard.importation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints usados para leitura e importação
 * dos arquivos Excel.
 */
@RestController
@RequestMapping("/import")
public class ImportController {

    private final ExcelFileReader excelFileReader;
    private final ColumnMappingService columnMappingService;
    private final ImportService importService;
    private final ImportHistoryService importHistoryService;

    public ImportController(
            ExcelFileReader excelFileReader,
            ColumnMappingService columnMappingService,
            ImportService importService,
            ImportHistoryService importHistoryService
    ) {
        this.excelFileReader = excelFileReader;
        this.columnMappingService = columnMappingService;
        this.importService = importService;
        this.importHistoryService = importHistoryService;
    }

    /**
     * Retorna somente os cabeçalhos encontrados no Excel.
     */
    @PostMapping("/headers")
    public List<String> headers(
            @RequestParam MultipartFile file
    ) throws IOException {

        return excelFileReader.readHeaders(file);
    }

    /**
     * Retorna os cabeçalhos e algumas linhas
     * para pré-visualização.
     */
    @PostMapping("/preview")
    public ExcelPreview preview(
            @RequestParam MultipartFile file
    ) throws IOException {

        return excelFileReader.readPreview(file);
    }

    /**
     * Mostra uma prévia das linhas após o mapeamento.
     */
    @PostMapping("/map-preview")
    public List<NormalizedDemandRow> mapPreview(

            @RequestParam MultipartFile file,

            @RequestParam(defaultValue = "")
            String externalId,

            @RequestParam(defaultValue = "")
            String protocolOnr,

            @RequestParam(defaultValue = "")
            String type,

            @RequestParam(defaultValue = "")
            String stage,

            @RequestParam(defaultValue = "")
            String responsible,

            @RequestParam(defaultValue = "")
            String status,

            @RequestParam(defaultValue = "")
            String entryDate,

            @RequestParam(defaultValue = "")
            String qualificationDate,

            @RequestParam(defaultValue = "")
            String deadline,

            @RequestParam(defaultValue = "")
            String reentryDate

    ) throws IOException {

        ExcelPreview preview =
                excelFileReader.readPreview(file);

        ColumnMapping mapping =
                createMapping(
                        externalId,
                        protocolOnr,
                        type,
                        stage,
                        responsible,
                        status,
                        entryDate,
                        qualificationDate,
                        deadline,
                        reentryDate
                );

        return columnMappingService.mapRows(
                preview,
                mapping
        );
    }

    /**
     * Importa todas as linhas válidas do Excel.
     *
     * referenceDate representa a data da situação.
     *
     * Se nenhuma data for enviada, utiliza
     * automaticamente o dia atual.
     */
    @PostMapping("/demands")
    public ImportResult importDemands(

            @RequestParam MultipartFile file,

            @RequestParam(defaultValue = "")
            String externalId,

            @RequestParam(defaultValue = "")
            String protocolOnr,

            @RequestParam(defaultValue = "")
            String type,

            @RequestParam(defaultValue = "")
            String stage,

            @RequestParam(defaultValue = "")
            String responsible,

            @RequestParam(defaultValue = "")
            String status,

            @RequestParam(defaultValue = "")
            String entryDate,

            @RequestParam(defaultValue = "")
            String qualificationDate,

            @RequestParam(defaultValue = "")
            String deadline,

            @RequestParam(defaultValue = "")
            String reentryDate,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate referenceDate

    ) throws IOException {

        ColumnMapping mapping =
                createMapping(
                        externalId,
                        protocolOnr,
                        type,
                        stage,
                        responsible,
                        status,
                        entryDate,
                        qualificationDate,
                        deadline,
                        reentryDate
                );

        LocalDate resolvedReferenceDate =
                referenceDate != null
                        ? referenceDate
                        : LocalDate.now();

        return importService.importDemands(
                file,
                mapping,
                resolvedReferenceDate
        );
    }

    /**
     * Retorna a importação mais recente.
     */
    @GetMapping("/history/latest")
    public ResponseEntity<ImportHistory> latestImport() {

        return importHistoryService
                .findLatest()
                .map(ResponseEntity::ok)
                .orElseGet(
                        () ->
                                ResponseEntity
                                        .noContent()
                                        .build()
                );
    }

    /**
     * Retorna as últimas 10 importações.
     */
    @GetMapping("/history")
    public List<ImportHistory> history() {

        return importHistoryService.findRecent();
    }

    /**
     * Apaga o histórico geral de importações.
     */
    @DeleteMapping("/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory() {

        importHistoryService.deleteAll();
    }

    /**
     * Cria o mapeamento entre as colunas do Excel
     * e os campos do sistema.
     */
    private ColumnMapping createMapping(

            String externalId,
            String protocolOnr,
            String type,
            String stage,
            String responsible,
            String status,
            String entryDate,
            String qualificationDate,
            String deadline,
            String reentryDate

    ) {

        return new ColumnMapping(
                externalId,
                protocolOnr,
                type,
                stage,
                responsible,
                status,
                entryDate,
                qualificationDate,
                deadline,
                reentryDate
        );
    }
}