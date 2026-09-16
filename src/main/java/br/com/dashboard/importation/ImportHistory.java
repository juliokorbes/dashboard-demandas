package br.com.dashboard.importation;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Registra informações sobre cada importação realizada.
 */
@Entity
@Table(name = "import_history")
public class ImportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(nullable = false)
    private int processed;

    @Column(nullable = false)
    private int imported;

    @Column(name = "duplicates", nullable = false)
    private int updated;

    @Column(nullable = false)
    private int skipped;

    @Column(nullable = false)
    private int errors;

    public ImportHistory() {
    }

    public ImportHistory(
            String fileName,
            LocalDateTime importedAt,
            int processed,
            int imported,
            int updated,
            int skipped,
            int errors
    ) {
        this.fileName = fileName;
        this.importedAt = importedAt;
        this.processed = processed;
        this.imported = imported;
        this.updated = updated;
        this.skipped = skipped;
        this.errors = errors;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public int getProcessed() {
        return processed;
    }

    public int getImported() {
        return imported;
    }

    public int getupdated() {
        return updated;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getErrors() {
        return errors;
    }
}