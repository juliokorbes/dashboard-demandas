package br.com.dashboard.importation;

import br.com.dashboard.demand.Demand;
import br.com.dashboard.demand.DemandService;
import br.com.dashboard.history.DemandSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Importa demandas de arquivos Excel.
 */
@Service
public class ImportService {

    private final ExcelFileReader excelFileReader;
    private final ColumnMappingService columnMappingService;
    private final DemandService demandService;
    private final ImportHistoryService importHistoryService;
    private final DemandSnapshotService demandSnapshotService;

    public ImportService(
            ExcelFileReader excelFileReader,
            ColumnMappingService columnMappingService,
            DemandService demandService,
            ImportHistoryService importHistoryService,
            DemandSnapshotService demandSnapshotService
    ) {
        this.excelFileReader = excelFileReader;
        this.columnMappingService = columnMappingService;
        this.demandService = demandService;
        this.importHistoryService = importHistoryService;
        this.demandSnapshotService = demandSnapshotService;
    }

    /**
     * Mantém compatibilidade com importações que
     * não informem data e hora da situação.
     *
     * Nesse caso, utiliza o momento atual.
     */
    public ImportResult importDemands(
            MultipartFile file,
            ColumnMapping mapping
    ) throws IOException {

        return importDemands(
                file,
                mapping,
                LocalDateTime.now()
        );
    }

    /**
     * Lê todas as linhas do Excel e salva ou atualiza
     * as demandas encontradas.
     *
     * Também cria ou atualiza uma fotografia histórica
     * para a data e hora da situação informada.
     *
     * Exemplo:
     *
     * 16/09/2026 14:00 + código 9101
     * representa uma fotografia.
     *
     * 16/09/2026 17:00 + código 9101
     * representa outra fotografia independente.
     */
    public ImportResult importDemands(
            MultipartFile file,
            ColumnMapping mapping,
            LocalDateTime referenceDateTime
    ) throws IOException {

        if (referenceDateTime == null) {
            throw new IllegalArgumentException(
                    "A data e hora da situação são obrigatórias."
            );
        }

        ExcelPreview excelData =
                excelFileReader.readAll(file);

        List<NormalizedDemandRow> rows =
                columnMappingService.mapRows(
                        excelData,
                        mapping
                );

        int processed = 0;
        int imported = 0;
        int updated = 0;
        int skipped = 0;

        List<String> errors =
                new ArrayList<>();

        for (
                int index = 0;
                index < rows.size();
                index++
        ) {

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
                                + ": código não informado."
                );

                continue;
            }

            try {

                /*
                 * A tabela principal representa
                 * a situação atual da dashboard.
                 *
                 * Se o código já existir, o mesmo
                 * registro é atualizado.
                 *
                 * Se ainda não existir, é criado
                 * um novo registro.
                 */
                Demand demand =
                        demandService
                                .findByExternalId(
                                        row.externalId()
                                )
                                .orElseGet(
                                        Demand::new
                                );

                boolean alreadyExists =
                        demand.getId() != null;

                updateDemand(
                        demand,
                        row
                );

                /*
                 * Salva a situação atual.
                 */
                demandService.save(
                        demand
                );

                /*
                 * Salva também a fotografia histórica
                 * correspondente à data e hora
                 * da situação.
                 *
                 * A combinação:
                 *
                 * data + hora da situação + código
                 *
                 * é única.
                 *
                 * Exemplos:
                 *
                 * 16/09/2026 14:00 + 9101
                 * cria uma fotografia.
                 *
                 * 16/09/2026 17:00 + 9101
                 * cria outra fotografia.
                 *
                 * 16/09/2026 17:00 + 9101
                 * novamente atualiza apenas a
                 * fotografia das 17:00.
                 */
                demandSnapshotService
                        .saveOrUpdate(
                                referenceDateTime,
                                demand
                        );

                if (alreadyExists) {
                    updated++;
                } else {
                    imported++;
                }

            } catch (Exception exception) {

                skipped++;

                String message =
                        exception.getMessage();

                if (
                        message == null ||
                                message.isBlank()
                ) {
                    message =
                            exception
                                    .getClass()
                                    .getSimpleName();
                }

                errors.add(
                        "Linha "
                                + (index + 2)
                                + ": "
                                + message
                );
            }
        }

        ImportResult result =
                new ImportResult(
                        processed,
                        imported,
                        updated,
                        skipped,
                        errors
                );

        /*
         * Salva o histórico geral da importação.
         */
        importHistoryService.register(
                file.getOriginalFilename(),
                result
        );

        return result;
    }

    /**
     * Atualiza uma demanda com os dados
     * mais recentes do Asgard.
     */
    private void updateDemand(
            Demand demand,
            NormalizedDemandRow row
    ) {

        demand.setExternalId(
                clean(
                        row.externalId()
                )
        );

        demand.setProtocolOnr(
                clean(
                        row.protocolOnr()
                )
        );

        demand.setType(
                clean(
                        row.type()
                )
        );

        String stage =
                clean(
                        row.stage()
                );

        /*
         * Mantém a etapa original exatamente
         * como veio do Asgard.
         */
        demand.setStage(
                stage
        );

        /*
         * Cria o agrupamento usado pela dashboard.
         */
        demand.setSector(
                classifySector(
                        stage
                )
        );

        demand.setResponsible(
                clean(
                        row.responsible()
                )
        );

        demand.setStatus(
                clean(
                        row.status()
                )
        );

        demand.setEntryDate(
                parseDate(
                        row.entryDate()
                )
        );

        /*
         * QUALIFICAÇÃO é a principal data
         * de prioridade operacional.
         */
        demand.setQualificationDate(
                parseDate(
                        row.qualificationDate()
                )
        );

        demand.setDeadline(
                parseDate(
                        row.deadline()
                )
        );

        demand.setReentryDate(
                parseDate(
                        row.reentryDate()
                )
        );

        /*
         * Campos antigos não são utilizados
         * na nova importação.
         */
        demand.setDescription(
                null
        );

        demand.setExternalUrl(
                null
        );
    }

    /**
     * Classifica a etapa original em um
     * grupo operacional da dashboard.
     */
    private String classifySector(
            String stage
    ) {

        if (
                stage == null ||
                        stage.isBlank()
        ) {
            return null;
        }

        String normalizedStage =
                normalizeText(
                        stage
                );

        /*
         * Conferência Inicial e
         * Conferência Inicial - ONR
         * pertencem ao mesmo grupo.
         */
        if (
                normalizedStage.contains(
                        "conferencia inicial"
                )
        ) {
            return "CONFERENCIA_INICIAL";
        }

        if (
                normalizedStage.contains(
                        "conferencia final"
                )
        ) {
            return "CONFERENCIA_FINAL";
        }

        return null;
    }

    /**
     * Remove acentos e padroniza o texto.
     */
    private String normalizeText(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return Normalizer
                .normalize(
                        value,
                        Normalizer.Form.NFD
                )
                .replaceAll(
                        "\\p{M}",
                        ""
                )
                .toLowerCase(
                        Locale.ROOT
                )
                .trim();
    }

    /**
     * Remove espaços extras.
     */
    private String clean(
            String value
    ) {

        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    /**
     * Converte os formatos de data
     * utilizados nos relatórios.
     */
    private LocalDate parseDate(
            String value
    ) {

        if (
                value == null ||
                        value.isBlank()
        ) {
            return null;
        }

        String date =
                value.trim();

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
                /*
                 * Tenta o próximo formato.
                 */
            }
        }

        throw new IllegalArgumentException(
                "Data inválida: "
                        + value
        );
    }

    /**
     * Verifica se a linha está completamente vazia.
     */
    private boolean isEmpty(
            NormalizedDemandRow row
    ) {

        return isBlank(
                row.externalId()
        )
                && isBlank(
                row.protocolOnr()
        )
                && isBlank(
                row.type()
        )
                && isBlank(
                row.stage()
        )
                && isBlank(
                row.responsible()
        )
                && isBlank(
                row.status()
        )
                && isBlank(
                row.entryDate()
        )
                && isBlank(
                row.qualificationDate()
        )
                && isBlank(
                row.deadline()
        )
                && isBlank(
                row.reentryDate()
        );
    }

    private boolean isBlank(
            String value
    ) {

        return value == null ||
                value.isBlank();
    }
}