package br.com.dashboard.importation;

/**
 * Define quais colunas do Excel correspondem aos campos usados pelo sistema.
 */
public record ColumnMapping(
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
}