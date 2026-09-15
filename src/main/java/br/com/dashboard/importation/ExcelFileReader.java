package br.com.dashboard.importation;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê arquivos Excel enviados para o sistema.
 */
@Component
public class ExcelFileReader {

    /**
     * Retorna os nomes das colunas.
     */
    public List<String> readHeaders(MultipartFile file) throws IOException {
        return readPreview(file).headers();
    }

    /**
     * Retorna até 5 linhas para pré-visualização.
     */
    public ExcelPreview readPreview(MultipartFile file) throws IOException {
        return read(file, 5);
    }

    /**
     * Retorna todas as linhas do arquivo.
     */
    public ExcelPreview readAll(MultipartFile file) throws IOException {
        return read(file, Integer.MAX_VALUE);
    }

    /**
     * Faz a leitura do Excel.
     */
    private ExcelPreview read(MultipartFile file, int maxRows) throws IOException {

        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            if (workbook.getNumberOfSheets() == 0) {
                return new ExcelPreview(headers, rows);
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null || headerRow.getLastCellNum() <= 0) {
                return new ExcelPreview(headers, rows);
            }

            int columnCount = headerRow.getLastCellNum();

            for (int i = 0; i < columnCount; i++) {
                headers.add(formatter.formatCellValue(headerRow.getCell(i)));
            }

            int lastRow = Math.min(sheet.getLastRowNum(), maxRows);

            for (int rowIndex = 1; rowIndex <= lastRow; rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null) {
                    continue;
                }

                List<String> values = new ArrayList<>();

                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                    values.add(
                            formatter.formatCellValue(row.getCell(columnIndex))
                    );
                }

                // Ignora linhas completamente vazias.
                if (values.stream().allMatch(String::isBlank)) {
                    continue;
                }

                rows.add(values);
            }
        }

        return new ExcelPreview(headers, rows);
    }
}