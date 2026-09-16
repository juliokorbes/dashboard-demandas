package br.com.dashboard.history;

import br.com.dashboard.demand.Demand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
            LocalDate referenceDate,
            Demand demand
    ) {

        if (referenceDate == null) {
            throw new IllegalArgumentException(
                    "A data da situação é obrigatória."
            );
        }

        if (
                demand == null ||
                        demand.getExternalId() == null ||
                        demand.getExternalId().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "A demanda precisa possuir um código."
            );
        }

        DemandSnapshot snapshot =
                repository
                        .findByReferenceDateAndExternalIdIgnoreCase(
                                referenceDate,
                                demand.getExternalId()
                        )
                        .orElseGet(
                                DemandSnapshot::new
                        );

        copyDemandToSnapshot(
                demand,
                snapshot,
                referenceDate
        );

        return repository.save(
                snapshot
        );
    }

    @Transactional
    public void saveOrUpdateAll(
            LocalDate referenceDate,
            List<Demand> demands
    ) {

        if (demands == null) {
            return;
        }

        for (Demand demand : demands) {

            saveOrUpdate(
                    referenceDate,
                    demand
            );
        }
    }

    @Transactional
    public void replaceSnapshot(
            LocalDate referenceDate,
            List<Demand> demands
    ) {

        if (referenceDate == null) {
            throw new IllegalArgumentException(
                    "A data da situação é obrigatória."
            );
        }

        repository.deleteAllByReferenceDate(
                referenceDate
        );

        if (demands == null) {
            return;
        }

        for (Demand demand : demands) {

            if (
                    demand == null ||
                            demand.getExternalId() == null ||
                            demand.getExternalId().isBlank()
            ) {
                continue;
            }

            DemandSnapshot snapshot =
                    new DemandSnapshot();

            copyDemandToSnapshot(
                    demand,
                    snapshot,
                    referenceDate
            );

            repository.save(
                    snapshot
            );
        }
    }

    public List<DemandSnapshot>
    findByReferenceDate(
            LocalDate referenceDate
    ) {

        return repository
                .findAllByReferenceDateOrderByQualificationDateAsc(
                        referenceDate
                );
    }

    public List<DemandSnapshot>
    findByReferenceDateAndSector(
            LocalDate referenceDate,
            String sector
    ) {

        return repository
                .findAllByReferenceDateAndSectorOrderByQualificationDateAsc(
                        referenceDate,
                        sector
                );
    }

    public boolean exists(
            LocalDate referenceDate,
            String externalId
    ) {

        if (
                referenceDate == null ||
                        externalId == null ||
                        externalId.isBlank()
        ) {
            return false;
        }

        return repository
                .existsByReferenceDateAndExternalIdIgnoreCase(
                        referenceDate,
                        externalId
                );
    }

    @Transactional
    public void deleteByReferenceDate(
            LocalDate referenceDate
    ) {

        if (referenceDate == null) {
            return;
        }

        repository.deleteAllByReferenceDate(
                referenceDate
        );
    }

    private void copyDemandToSnapshot(
            Demand demand,
            DemandSnapshot snapshot,
            LocalDate referenceDate
    ) {

        snapshot.setReferenceDate(
                referenceDate
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
    }
}