package br.com.dashboard.importation;

import java.util.List;

/**
 * Representa o resultado de uma importação.
 */
public record ImportResult(
        int processed,
        int imported,
        int duplicates,
        int skipped,
        List<String> errors
) {
}