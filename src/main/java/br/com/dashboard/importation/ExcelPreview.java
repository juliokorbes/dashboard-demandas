package br.com.dashboard.importation;

import java.util.List;

/**
 * Representa uma prévia dos dados do arquivo Excel.
 */
public record ExcelPreview(
        List<String> headers,
        List<List<String>> rows
) {
}