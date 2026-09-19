package br.com.dashboard.importation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
     * Retorna somente os cabeçalhos encontrados
     * no primeiro arquivo selecionado.
     *
     * O mapeamento é utilizado para todos os arquivos
     * do mesmo lote.
     */
    @PostMapping("/headers")
    public List<String> headers(
            @RequestParam MultipartFile file
    ) throws IOException {

        return excelFileReader.readHeaders(
                file
        );
    }

    /**
     * Retorna os cabeçalhos e algumas linhas
     * para pré-visualização do primeiro arquivo.
     */
    @PostMapping("/preview")
    public ExcelPreview preview(
            @RequestParam MultipartFile file
    ) throws IOException {

        return excelFileReader.readPreview(
                file
        );
    }

    /**
     * Mostra uma prévia das linhas
     * após o mapeamento.
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
                excelFileReader.readPreview(
                        file
                );

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
     * Importa um ou vários arquivos
     * como uma única fotografia.
     *
     * Compatibilidade:
     *
     * - "file" aceita o frontend antigo
     *   com apenas um arquivo.
     *
     * - "files" aceita vários arquivos
     *   no mesmo lote.
     *
     * Todos representam a mesma data e hora.
     */
    @PostMapping("/demands")
    public ImportResult importDemands(

            @RequestParam(
                    name = "file",
                    required = false
            )
            MultipartFile file,

            @RequestParam(
                    name = "files",
                    required = false
            )
            List<MultipartFile> files,

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
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime referenceDateTime

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

        LocalDateTime resolvedReferenceDateTime =
                referenceDateTime != null
                        ? referenceDateTime
                        : LocalDateTime.now();

        List<MultipartFile> resolvedFiles =
                resolveFiles(
                        file,
                        files
                );

        if (resolvedFiles.isEmpty()) {
            throw new IllegalArgumentException(
                    "Selecione pelo menos um arquivo."
            );
        }

        return importService.importDemands(
                resolvedFiles,
                mapping,
                resolvedReferenceDateTime
        );
    }

    /**
     * Retorna a importação mais recente.
     */
    @GetMapping("/history/latest")
    public ResponseEntity<ImportHistory> latestImport() {

        return importHistoryService
                .findLatest()
                .map(
                        ResponseEntity::ok
                )
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
     * Apaga o histórico técnico
     * de importações.
     */
    @DeleteMapping("/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistory() {

        importHistoryService.deleteAll();
    }

    /**
     * Aceita tanto o formato antigo:
     *
     * file = arquivo único
     *
     * quanto o novo:
     *
     * files = vários arquivos.
     */
    private List<MultipartFile> resolveFiles(
            MultipartFile file,
            List<MultipartFile> files
    ) {

        List<MultipartFile> resolved =
                new ArrayList<>();

        if (files != null) {

            for (MultipartFile item : files) {

                if (
                        item != null &&
                                !item.isEmpty()
                ) {
                    resolved.add(
                            item
                    );
                }
            }
        }

        if (
                file != null &&
                        !file.isEmpty()
        ) {

            resolved.add(
                    file
            );
        }

        return resolved;
    }

    /**
     * Cria o mapeamento entre as colunas
     * do Excel e os campos do sistema.
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