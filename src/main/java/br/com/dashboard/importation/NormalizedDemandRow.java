package br.com.dashboard.importation;

/**
 * Representa uma linha do Excel já convertida para os campos usados pelo sistema.
 */
public record NormalizedDemandRow(
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