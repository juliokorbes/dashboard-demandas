package br.com.dashboard.dashboard;

/**
 * Representa a quantidade de demandas por categoria.
 */
public record CategoryCount(
        String category,
        long count
) {
}