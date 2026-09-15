package br.com.dashboard.importation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Endpoints usados para leitura e importação dos arquivos Excel.
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
            String type,

            @RequestParam(defaultValue = "")
            String sector,

            @RequestParam(defaultValue = "")
            String responsible,

            @RequestParam(defaultValue = "")
            String entryDate,

            @RequestParam(defaultValue = "")
            String deadline,

            @RequestParam(defaultValue = "")
            String status,

            @RequestParam(defaultValue = "")
            String description

    ) throws IOException {

        ExcelPreview preview =
                excelFileReader.readPreview(file);

        ColumnMapping mapping =
                createMapping(
                        externalId,
                        type,
                        sector,
                        responsible,
                        entryDate,
                        deadline,
                        status,
                        description
                );

        return columnMappingService.mapRows(
                preview,
                mapping
        );
    }

    /**
     * Importa todas as linhas válidas do Excel.
     */
    @PostMapping("/demands")
    public ImportResult importDemands(

            @RequestParam MultipartFile file,

            @RequestParam(defaultValue = "")
            String externalId,

            @RequestParam(defaultValue = "")
            String type,

            @RequestParam(defaultValue = "")
            String sector,

            @RequestParam(defaultValue = "")
            String responsible,

            @RequestParam(defaultValue = "")
            String entryDate,

            @RequestParam(defaultValue = "")
            String deadline,

            @RequestParam(defaultValue = "")
            String status,

            @RequestParam(defaultValue = "")
            String description

    ) throws IOException {

        ColumnMapping mapping =
                createMapping(
                        externalId,
                        type,
                        sector,
                        responsible,
                        entryDate,
                        deadline,
                        status,
                        description
                );

        return importService.importDemands(
                file,
                mapping
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
     * Apaga o histórico de importações.
     */
    @DeleteMapping("/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory() {

        importHistoryService.deleteAll();
    }

    private ColumnMapping createMapping(

            String externalId,
            String type,
            String sector,
            String responsible,
            String entryDate,
            String deadline,
            String status,
            String description

    ) {

        return new ColumnMapping(
                externalId,
                type,
                sector,
                responsible,
                entryDate,
                deadline,
                status,
                description,
                null
        );
    }
}