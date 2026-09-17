package br.com.dashboard.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DemandSnapshotRepository
        extends JpaRepository<DemandSnapshot, Long> {

    Optional<DemandSnapshot>
    findByReferenceDateTimeAndExternalIdIgnoreCase(
            LocalDateTime referenceDateTime,
            String externalId
    );

    List<DemandSnapshot>
    findAllByReferenceDateTimeOrderByQualificationDateAsc(
            LocalDateTime referenceDateTime
    );

    List<DemandSnapshot>
    findAllByReferenceDateTimeAndSectorOrderByQualificationDateAsc(
            LocalDateTime referenceDateTime,
            String sector
    );

    boolean existsByReferenceDateTimeAndExternalIdIgnoreCase(
            LocalDateTime referenceDateTime,
            String externalId
    );

    void deleteAllByReferenceDateTime(
            LocalDateTime referenceDateTime
    );

    @Query("""
            select distinct snapshot.referenceDateTime
            from DemandSnapshot snapshot
            order by snapshot.referenceDateTime desc
            """)
    List<LocalDateTime> findDistinctReferenceDateTimes();
}