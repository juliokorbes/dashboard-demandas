package br.com.dashboard.importation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportHistoryRepository
        extends JpaRepository<ImportHistory, Long> {

    Optional<ImportHistory> findTopByOrderByImportedAtDesc();

    List<ImportHistory> findTop10ByOrderByImportedAtDesc();
}