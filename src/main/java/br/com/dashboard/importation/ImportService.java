package br.com.dashboard.importation;

import br.com.dashboard.demand.Demand;
import br.com.dashboard.demand.DemandService;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Importa as demandas encontradas em um arquivo Excel.
 */
@Service
public class ImportService {

    private final ExcelFileReader excelFileReader;

    private final ColumnMappingService columnMappingService;

    private final DemandService demandService;

    private final ImportHistoryService importHistoryService;

    public ImportService(
            ExcelFileReader excelFileReader,
            ColumnMappingService columnMappingService,
            DemandService demandService,
            ImportHistoryService importHistoryService
    ) {
        this.excelFileReader = excelFileReader;
        this.columnMappingService = columnMappingService;
        this.demandService = demandService;
        this.importHistoryService = importHistoryService;
    }

    /**
     * Lê todas as linhas do Excel e salva as demandas válidas.
     */
    public ImportResult importDemands(
            MultipartFile file,
            ColumnMapping mapping
    ) throws IOException {

        ExcelPreview excelData =
                excelFileReader.readAll(file);

        List<NormalizedDemandRow> rows =
                columnMappingService.mapRows(
                        excelData,
                        mapping
                );

        int processed = 0;
        int imported = 0;
        int duplicates = 0;
        int skipped = 0;

        List<String> errors = new ArrayList<>();

        for (int index = 0;
             index < rows.size();
             index++) {

            NormalizedDemandRow row =
                    rows.get(index);

            processed++;

            if (isEmpty(row)) {
                skipped++;
                continue;
            }

            if (
                    row.externalId() == null ||
                            row.externalId().isBlank()
            ) {

                skipped++;

                errors.add(
                        "Linha "
                                + (index + 2)
                                + ": protocolo/identificador não informado."
                );

                continue;
            }

            if (
                    demandService.existsByExternalId(
                            row.externalId()
                    )
            ) {

                duplicates++;

                continue;
            }

            try {

                Demand demand =
                        convertToDemand(row);

                demandService.save(demand);

                imported++;

            } catch (Exception exception) {

                skipped++;

                errors.add(
                        "Linha "
                                + (index + 2)
                                + ": "
                                + exception.getMessage()
                );
            }
        }

        ImportResult result =
                new ImportResult(
                        processed,
                        imported,
                        duplicates,
                        skipped,
                        errors
                );

        /*
         * Registra no SQLite as informações
         * desta importação.
         */
        importHistoryService.register(
                file.getOriginalFilename(),
                result
        );

        return result;
    }

    /**
     * Converte uma linha normalizada em uma demanda.
     */
    private Demand convertToDemand(
            NormalizedDemandRow row
    ) {

        Demand demand = new Demand();

        demand.setExternalId(
                clean(row.externalId())
        );

        demand.setType(
                clean(row.type())
        );

        demand.setSector(
                clean(row.sector())
        );

        demand.setResponsible(
                clean(row.responsible())
        );

        demand.setEntryDate(
                parseDate(row.entryDate())
        );

        demand.setDeadline(
                parseDate(row.deadline())
        );

        demand.setStatus(
                clean(row.status())
        );

        demand.setDescription(
                clean(row.description())
        );

        /*
         * Não utilizamos integração ou link
         * para sistemas externos.
         */
        demand.setExternalUrl(null);

        return demand;
    }

    /**
     * Remove espaços extras.
     */
    private String clean(String value) {

        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    /**
     * Converte os formatos de data mais comuns.
     */
    private LocalDate parseDate(String value) {

        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        String date = value.trim();

        List<DateTimeFormatter> formatters =
                List.of(
                        DateTimeFormatter.ISO_LOCAL_DATE,

                        DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy"
                        ),

                        DateTimeFormatter.ofPattern(
                                "d/M/yyyy"
                        ),

                        DateTimeFormatter.ofPattern(
                                "dd-MM-yyyy"
                        ),

                        DateTimeFormatter.ofPattern(
                                "d-M-yyyy"
                        )
                );

        for (
                DateTimeFormatter formatter :
                formatters
        ) {

            try {

                return LocalDate.parse(
                        date,
                        formatter
                );

            } catch (
                    DateTimeParseException ignored
            ) {
                // Tenta o próximo formato.
            }
        }

        throw new IllegalArgumentException(
                "Data inválida: " + value
        );
    }

    /**
     * Verifica se a linha está completamente vazia.
     */
    private boolean isEmpty(
            NormalizedDemandRow row
    ) {

        return isBlank(row.externalId())
                && isBlank(row.type())
                && isBlank(row.sector())
                && isBlank(row.responsible())
                && isBlank(row.entryDate())
                && isBlank(row.deadline())
                && isBlank(row.status())
                && isBlank(row.description())
                && isBlank(row.externalUrl());
    }

    private boolean isBlank(String value) {

        return value == null ||
                value.isBlank();
    }
}