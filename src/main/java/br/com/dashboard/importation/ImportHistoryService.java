package br.com.dashboard.importation;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Gerencia o histórico das importações.
 */
@Service
public class ImportHistoryService {

    private final ImportHistoryRepository importHistoryRepository;

    public ImportHistoryService(
            ImportHistoryRepository importHistoryRepository
    ) {
        this.importHistoryRepository = importHistoryRepository;
    }

    /**
     * Registra o resultado de uma importação.
     */
    public ImportHistory register(
            String fileName,
            ImportResult result
    ) {

        String safeFileName =
                fileName == null || fileName.isBlank()
                        ? "arquivo.xlsx"
                        : fileName;

        ImportHistory history =
                new ImportHistory(
                        safeFileName,
                        LocalDateTime.now(),
                        result.processed(),
                        result.imported(),
                        result.duplicates(),
                        result.skipped(),
                        result.errors().size()
                );

        return importHistoryRepository.save(history);
    }

    /**
     * Retorna a importação mais recente.
     */
    public Optional<ImportHistory> findLatest() {
        return importHistoryRepository
                .findTopByOrderByImportedAtDesc();
    }

    /**
     * Retorna as últimas importações.
     */
    public List<ImportHistory> findRecent() {
        return importHistoryRepository
                .findTop10ByOrderByImportedAtDesc();
    }

    /**
     * Apaga o histórico das importações.
     */
    public void deleteAll() {
        importHistoryRepository.deleteAll();
    }
}