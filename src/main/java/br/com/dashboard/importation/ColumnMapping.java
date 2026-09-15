package br.com.dashboard.importation;

/**
 * Define como as colunas do arquivo serão mapeadas.
 */
public record ColumnMapping(
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