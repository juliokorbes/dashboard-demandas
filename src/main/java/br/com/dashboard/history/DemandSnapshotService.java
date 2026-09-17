package br.com.dashboard.history;

import br.com.dashboard.demand.Demand;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DemandSnapshotService {

    private final DemandSnapshotRepository repository;

    public DemandSnapshotService(
            DemandSnapshotRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional
    public DemandSnapshot saveOrUpdate(
            LocalDateTime referenceDateTime,
            Demand demand
    ) {
        DemandSnapshot snapshot =
                repository
                        .findByReferenceDateTimeAndExternalIdIgnoreCase(
                                referenceDateTime,
                                demand.getExternalId()
                        )
                        .orElseGet(DemandSnapshot::new);

        snapshot.setReferenceDateTime(
                referenceDateTime
        );

        snapshot.setExternalId(
                demand.getExternalId()
        );

        snapshot.setProtocolOnr(
                demand.getProtocolOnr()
        );

        snapshot.setType(
                demand.getType()
        );

        snapshot.setStage(
                demand.getStage()
        );

        snapshot.setSector(
                demand.getSector()
        );

        snapshot.setResponsible(
                demand.getResponsible()
        );

        snapshot.setEntryDate(
                demand.getEntryDate()
        );

        snapshot.setQualificationDate(
                demand.getQualificationDate()
        );

        snapshot.setDeadline(
                demand.getDeadline()
        );

        snapshot.setReentryDate(
                demand.getReentryDate()
        );

        snapshot.setStatus(
                demand.getStatus()
        );

        return repository.save(snapshot);
    }

    @Transactional
    public void saveOrUpdateAll(
            LocalDateTime referenceDateTime,
            List<Demand> demands
    ) {
        for (Demand demand : demands) {
            saveOrUpdate(
                    referenceDateTime,
                    demand
            );
        }
    }

    @Transactional
    public void replaceSnapshot(
            LocalDateTime referenceDateTime,
            List<Demand> demands
    ) {
        repository.deleteAllByReferenceDateTime(
                referenceDateTime
        );

        saveOrUpdateAll(
                referenceDateTime,
                demands
        );
    }

    public List<DemandSnapshot> findByReferenceDateTime(
            LocalDateTime referenceDateTime
    ) {
        return repository
                .findAllByReferenceDateTimeOrderByQualificationDateAsc(
                        referenceDateTime
                );
    }

    public List<DemandSnapshot> findByReferenceDateTimeAndSector(
            LocalDateTime referenceDateTime,
            String sector
    ) {
        return repository
                .findAllByReferenceDateTimeAndSectorOrderByQualificationDateAsc(
                        referenceDateTime,
                        sector
                );
    }

    public List<LocalDateTime> findAvailableDateTimes() {
        return repository
                .findDistinctReferenceDateTimes();
    }

    public boolean exists(
            LocalDateTime referenceDateTime,
            String externalId
    ) {
        return repository
                .existsByReferenceDateTimeAndExternalIdIgnoreCase(
                        referenceDateTime,
                        externalId
                );
    }

    @Transactional
    public void deleteByReferenceDateTime(
            LocalDateTime referenceDateTime
    ) {
        repository.deleteAllByReferenceDateTime(
                referenceDateTime
        );
    }
}