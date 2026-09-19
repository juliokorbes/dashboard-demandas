package br.com.dashboard.importation;

import br.com.dashboard.demand.Demand;
import br.com.dashboard.demand.DemandService;
import br.com.dashboard.history.DemandSnapshot;
import br.com.dashboard.history.DemandSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Importa demandas dos relatórios Excel.
 *
 * Cada lote de arquivos representa uma fotografia
 * completa das filas monitoradas em determinada
 * data e hora.
 */
@Service
public class ImportService {

    /*
     * Datas modernas armazenadas pelo Excel possuem
     * números seriais muito superiores a isso.
     *
     * Valores pequenos como 32 não devem ser
     * interpretados como 31/01/1900.
     */
    private static final long MIN_VALID_EXCEL_SERIAL = 10000;

    private static final long MAX_VALID_EXCEL_SERIAL = 100000;

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
     * Mantém compatibilidade com importação
     * de apenas um arquivo sem data informada.
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importDemands(
            MultipartFile file,
            ColumnMapping mapping
    ) throws IOException {

        return importDemands(
                List.of(file),
                mapping,
                LocalDateTime.now()
        );
    }

    /**
     * Mantém compatibilidade com importação
     * de apenas um arquivo com data e hora.
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importDemands(
            MultipartFile file,
            ColumnMapping mapping,
            LocalDateTime referenceDateTime
    ) throws IOException {

        return importDemands(
                List.of(file),
                mapping,
                referenceDateTime
        );
    }

    /**
     * Importa vários arquivos como um único lote.
     *
     * Todos os arquivos selecionados representam
     * a mesma fotografia das filas naquele horário.
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importDemands(
            List<MultipartFile> files,
            ColumnMapping mapping,
            LocalDateTime referenceDateTime
    ) throws IOException {

        if (referenceDateTime == null) {
            throw new IllegalArgumentException(
                    "A data e hora da situação são obrigatórias."
            );
        }

        if (
                files == null ||
                        files.isEmpty()
        ) {
            throw new IllegalArgumentException(
                    "Nenhum arquivo foi informado."
            );
        }

        /*
         * Guarda a situação existente antes
         * da nova importação.
         *
         * Isso permite calcular o que é novo
         * e o que permaneceu na fila.
         */
        List<Demand> currentBeforeImport =
                demandService.findAll();

        Map<String, Demand> currentByCode =
                new LinkedHashMap<>();

        for (Demand demand : currentBeforeImport) {

            if (
                    demand.getExternalId() != null &&
                            !demand.getExternalId().isBlank()
            ) {
                currentByCode.put(
                        normalizeCode(
                                demand.getExternalId()
                        ),
                        demand
                );
            }
        }

        /*
         * Se já existir uma fotografia exatamente
         * nesse horário, guardamos seus códigos antes
         * de substituí-la.
         *
         * Isso é importante para reimportações
         * do mesmo horário.
         */
        Map<String, DemandSnapshot> existingSnapshotByCode =
                new LinkedHashMap<>();

        List<DemandSnapshot> existingSnapshot =
                demandSnapshotService
                        .findByReferenceDateTime(
                                referenceDateTime
                        );

        for (DemandSnapshot snapshot : existingSnapshot) {

            if (
                    snapshot.getExternalId() != null &&
                            !snapshot.getExternalId().isBlank()
            ) {
                existingSnapshotByCode.put(
                        normalizeCode(
                                snapshot.getExternalId()
                        ),
                        snapshot
                );
            }
        }

        /*
         * Todos os arquivos são lidos antes
         * de qualquer alteração no banco.
         *
         * O mapa consolida registros pelo CÓDIGO.
         */
        Map<String, NormalizedDemandRow> rowsByCode =
                new LinkedHashMap<>();

        int processed = 0;
        int skipped = 0;

        List<String> errors =
                new ArrayList<>();

        for (MultipartFile file : files) {

            if (
                    file == null ||
                            file.isEmpty()
            ) {
                skipped++;
                continue;
            }

            ExcelPreview excelData =
                    excelFileReader.readAll(file);

            List<NormalizedDemandRow> rows =
                    columnMappingService.mapRows(
                            excelData,
                            mapping
                    );

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
                            fileName(file)
                                    + " - linha "
                                    + (index + 2)
                                    + ": código não informado."
                    );

                    continue;
                }

                String normalizedCode =
                        normalizeCode(
                                row.externalId()
                        );

                /*
                 * Caso o mesmo código apareça em mais
                 * de um arquivo do mesmo lote,
                 * consolidamos as informações.
                 */
                NormalizedDemandRow previousRow =
                        rowsByCode.get(
                                normalizedCode
                        );

                if (previousRow == null) {

                    rowsByCode.put(
                            normalizedCode,
                            row
                    );

                } else {

                    rowsByCode.put(
                            normalizedCode,
                            mergeRows(
                                    previousRow,
                                    row
                            )
                    );
                }
            }
        }

        /*
         * Converte a fotografia consolidada
         * em objetos de domínio.
         */
        List<Demand> snapshotDemands =
                new ArrayList<>();

        int imported = 0;
        int updated = 0;

        for (
                Map.Entry<String, NormalizedDemandRow> entry :
                rowsByCode.entrySet()
        ) {

            NormalizedDemandRow row =
                    entry.getValue();

            try {

                Demand demand =
                        new Demand();

                updateDemand(
                        demand,
                        row
                );

                snapshotDemands.add(
                        demand
                );

                /*
                 * Para uma reimportação do mesmo horário,
                 * usamos a fotografia daquele horário
                 * para calcular novo/atualizado.
                 *
                 * Para uma fotografia nova,
                 * usamos a situação atual anterior.
                 */
                boolean alreadyExists;

                if (!existingSnapshotByCode.isEmpty()) {

                    alreadyExists =
                            existingSnapshotByCode
                                    .containsKey(
                                            entry.getKey()
                                    );

                } else {

                    alreadyExists =
                            currentByCode
                                    .containsKey(
                                            entry.getKey()
                                    );
                }

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
                        "Código "
                                + row.externalId()
                                + ": "
                                + message
                );
            }
        }

        /*
         * A fotografia daquele horário é sempre
         * substituída completamente.
         *
         * Portanto, importar novamente 15:00
         * atualiza a fotografia das 15:00,
         * sem gerar duplicidade.
         */
        demandSnapshotService
                .replaceSnapshot(
                        referenceDateTime,
                        snapshotDemands
                );

        /*
         * Descobre qual é a fotografia cronologicamente
         * mais recente depois da importação.
         */
        List<LocalDateTime> availableDateTimes =
                demandSnapshotService
                        .findAvailableDateTimes();

        LocalDateTime latestReferenceDateTime =
                availableDateTimes.isEmpty()
                        ? referenceDateTime
                        : availableDateTimes.get(0);

        /*
         * SOMENTE a fotografia mais recente
         * pode representar a Situação Atual.
         *
         * Assim, importar posteriormente um arquivo
         * antigo não faz a dashboard voltar no tempo.
         */
        if (
                referenceDateTime.equals(
                        latestReferenceDateTime
                )
        ) {

            /*
             * A situação atual é substituída
             * completamente pela nova fotografia.
             *
             * O que não aparece mais deixa de fazer
             * parte da fila atual.
             *
             * Não classificamos automaticamente
             * essas demandas como concluídas.
             *
             * Elas permanecem registradas nos
             * snapshots históricos anteriores.
             */
            demandService
                    .replaceCurrentSituation(
                            snapshotDemands
                    );
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
         * Registra uma única importação para o lote.
         */
        importHistoryService.register(
                describeFiles(files),
                result
        );

        return result;
    }

    /**
     * Consolida duas linhas que possuem
     * o mesmo código.
     *
     * Informações não vazias da linha mais recente
     * prevalecem sobre as anteriores.
     */
    private NormalizedDemandRow mergeRows(
            NormalizedDemandRow previous,
            NormalizedDemandRow current
    ) {

        return new NormalizedDemandRow(
                preferCurrent(
                        previous.externalId(),
                        current.externalId()
                ),
                preferCurrent(
                        previous.protocolOnr(),
                        current.protocolOnr()
                ),
                preferCurrent(
                        previous.type(),
                        current.type()
                ),
                preferCurrent(
                        previous.stage(),
                        current.stage()
                ),
                preferCurrent(
                        previous.responsible(),
                        current.responsible()
                ),
                preferCurrent(
                        previous.status(),
                        current.status()
                ),
                preferCurrent(
                        previous.entryDate(),
                        current.entryDate()
                ),
                preferCurrent(
                        previous.qualificationDate(),
                        current.qualificationDate()
                ),
                preferCurrent(
                        previous.deadline(),
                        current.deadline()
                ),
                preferCurrent(
                        previous.reentryDate(),
                        current.reentryDate()
                )
        );
    }

    private String preferCurrent(
            String previous,
            String current
    ) {

        if (
                current != null &&
                        !current.isBlank()
        ) {
            return current;
        }

        return previous;
    }

    /**
     * Atualiza uma demanda com os dados
     * provenientes do relatório.
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
         * Preserva exatamente a etapa recebida:
         *
         * Conferência Inicial
         * Conferência Inicial - ONR
         * Conferência Final
         */
        demand.setStage(
                stage
        );

        /*
         * Agrupamento utilizado pela dashboard.
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
         * QUALIFICAÇÃO permanece como principal
         * data de prioridade operacional.
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

        demand.setDescription(
                null
        );

        demand.setExternalUrl(
                null
        );
    }

    /**
     * Conferência Inicial e Conferência Inicial - ONR
     * pertencem ao mesmo grupo operacional.
     *
     * A etapa original continua preservada
     * no campo stage.
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
     * Converte datas vindas dos relatórios.
     *
     * Aceita:
     *
     * yyyy-MM-dd
     * dd/MM/yyyy
     * d/M/yyyy
     * dd-MM-yyyy
     * d-M-yyyy
     *
     * Também aceita números seriais válidos
     * utilizados internamente pelo Excel.
     *
     * Valores numéricos muito pequenos, como 32,
     * são tratados como campo vazio para impedir
     * datas artificiais como 31/01/1900.
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

        /*
         * Alguns arquivos Excel podem entregar
         * a data como seu número serial interno.
         */
        try {

            String numericValue =
                    date.replace(
                            ",",
                            "."
                    );

            double serial =
                    Double.parseDouble(
                            numericValue
                    );

            long days =
                    (long) Math.floor(
                            serial
                    );

            /*
             * Números muito pequenos normalmente
             * representam lixo de planilha ou outro
             * tipo de valor, e não uma data operacional.
             */
            if (
                    days > 0 &&
                            days < MIN_VALID_EXCEL_SERIAL
            ) {
                return null;
            }

            if (
                    days >= MIN_VALID_EXCEL_SERIAL &&
                            days < MAX_VALID_EXCEL_SERIAL
            ) {

                return LocalDate
                        .of(
                                1899,
                                12,
                                30
                        )
                        .plusDays(
                                days
                        );
            }

        } catch (NumberFormatException ignored) {
            /*
             * Não é número serial.
             * Tenta os formatos textuais abaixo.
             */
        }

        /*
         * Alguns leitores podem retornar
         * data e hora completas.
         */
        if (
                date.length() >= 10 &&
                        date.charAt(4) == '-' &&
                        date.charAt(7) == '-'
        ) {

            try {

                LocalDate parsedDate =
                        LocalDate.parse(
                                date.substring(
                                        0,
                                        10
                                ),
                                DateTimeFormatter.ISO_LOCAL_DATE
                        );

                return sanitizeDate(
                        parsedDate
                );

            } catch (
                    DateTimeParseException ignored
            ) {
                /*
                 * Continua para os demais formatos.
                 */
            }
        }

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

                LocalDate parsedDate =
                        LocalDate.parse(
                                date,
                                formatter
                        );

                return sanitizeDate(
                        parsedDate
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
     * Remove datas evidentemente artificiais
     * que podem surgir de células defeituosas.
     */
    private LocalDate sanitizeDate(
            LocalDate date
    ) {

        if (date == null) {
            return null;
        }

        /*
         * Evita datas do começo do calendário do Excel,
         * como 1900 e 1901.
         */
        if (date.getYear() <= 1901) {
            return null;
        }

        return date;
    }

    /**
     * Remove acentos e padroniza texto.
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
     * Código usado como chave de comparação.
     */
    private String normalizeCode(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
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

    private String fileName(
            MultipartFile file
    ) {

        if (
                file.getOriginalFilename() == null ||
                        file.getOriginalFilename().isBlank()
        ) {
            return "Arquivo";
        }

        return file.getOriginalFilename();
    }

    /**
     * Texto curto usado no histórico técnico
     * de importações.
     */
    private String describeFiles(
            List<MultipartFile> files
    ) {

        if (files.size() == 1) {
            return fileName(
                    files.get(0)
            );
        }

        return files.size()
                + " arquivos - lote";
    }
}