package br.com.dashboard.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DemandSnapshotRepository
        extends JpaRepository<DemandSnapshot, Long> {

    Optional<DemandSnapshot>
    findByReferenceDateAndExternalIdIgnoreCase(
            LocalDate referenceDate,
            String externalId
    );

    List<DemandSnapshot>
    findAllByReferenceDateOrderByQualificationDateAsc(
            LocalDate referenceDate
    );

    List<DemandSnapshot>
    findAllByReferenceDateAndSectorOrderByQualificationDateAsc(
            LocalDate referenceDate,
            String sector
    );

    boolean existsByReferenceDateAndExternalIdIgnoreCase(
            LocalDate referenceDate,
            String externalId
    );

    void deleteAllByReferenceDate(
            LocalDate referenceDate
    );
}