package br.com.dashboard.importation;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Converte colunas do arquivo Excel para o formato interno do sistema.
 */
@Service
public class ColumnMappingService {

    /**
     * Converte as linhas usando o mapeamento informado.
     */
    public List<NormalizedDemandRow> mapRows(
            ExcelPreview data,
            ColumnMapping mapping
    ) {

        return data.rows()
                .stream()
                .map(row -> new NormalizedDemandRow(
                        getValue(data.headers(), row, mapping.externalId()),
                        getValue(data.headers(), row, mapping.protocolOnr()),
                        getValue(data.headers(), row, mapping.type()),
                        getValue(data.headers(), row, mapping.stage()),
                        getValue(data.headers(), row, mapping.responsible()),
                        getValue(data.headers(), row, mapping.status()),
                        getValue(data.headers(), row, mapping.entryDate()),
                        getValue(data.headers(), row, mapping.qualificationDate()),
                        getValue(data.headers(), row, mapping.deadline()),
                        getValue(data.headers(), row, mapping.reentryDate())
                ))
                .toList();
    }

    /**
     * Busca o valor da coluna correspondente.
     */
    private String getValue(
            List<String> headers,
            List<String> row,
            String sourceColumn
    ) {

        if (sourceColumn == null || sourceColumn.isBlank()) {
            return null;
        }

        int index = headers.indexOf(sourceColumn);

        if (index < 0) {
            throw new IllegalArgumentException(
                    "Coluna não encontrada: " + sourceColumn
            );
        }

        if (index >= row.size()) {
            return null;
        }

        return row.get(index);
    }
}