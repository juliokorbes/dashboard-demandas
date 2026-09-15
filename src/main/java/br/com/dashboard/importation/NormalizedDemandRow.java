package br.com.dashboard.importation;

/**
 * Representa uma linha já convertida para o formato interno.
 */
public record NormalizedDemandRow(
        String externalId,
        String type,
        String sector,
        String responsible,
        String entryDate,
        String deadline,
        String status,
        String description,
        String externalUrl
) {
}