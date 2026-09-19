package br.com.dashboard.history;

import br.com.dashboard.demand.Demand;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DemandSnapshotService {

    private final DemandSnapshotRepository repository;

    public DemandSnapshotService(
            DemandSnapshotRepository repository
    ) {
        this.repository = repository;
    }

    /**
     * Cria ou atualiza uma demanda dentro
     * de uma fotografia específica.
     */
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
                        .orElseGet(
                                DemandSnapshot::new
                        );

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

        return repository.save(
                snapshot
        );
    }

    /**
     * Salva várias demandas dentro
     * da mesma fotografia.
     */
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

    /**
     * Substitui completamente uma fotografia.
     *
     * Isso permite reimportar exatamente
     * a mesma data e hora sem duplicar dados.
     */
    @Transactional
    public void replaceSnapshot(
            LocalDateTime referenceDateTime,
            List<Demand> demands
    ) {

        repository
                .deleteAllByReferenceDateTime(
                        referenceDateTime
                );

        repository.flush();

        saveOrUpdateAll(
                referenceDateTime,
                demands
        );
    }

    /**
     * Retorna todas as demandas existentes
     * em determinada fotografia.
     */
    public List<DemandSnapshot> findByReferenceDateTime(
            LocalDateTime referenceDateTime
    ) {

        return repository
                .findAllByReferenceDateTimeOrderByQualificationDateAsc(
                        referenceDateTime
                );
    }

    /**
     * Retorna uma fotografia filtrada
     * pelo grupo operacional.
     */
    public List<DemandSnapshot>
    findByReferenceDateTimeAndSector(
            LocalDateTime referenceDateTime,
            String sector
    ) {

        return repository
                .findAllByReferenceDateTimeAndSectorOrderByQualificationDateAsc(
                        referenceDateTime,
                        sector
                );
    }

    /**
     * Retorna todas as datas e horários disponíveis,
     * da mais recente para a mais antiga.
     */
    public List<LocalDateTime>
    findAvailableDateTimes() {

        return repository
                .findDistinctReferenceDateTimes();
    }

    /**
     * Retorna a fotografia mais recente.
     */
    public Optional<LocalDateTime>
    findLatestDateTime() {

        List<LocalDateTime> dateTimes =
                findAvailableDateTimes();

        if (dateTimes.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                dateTimes.get(0)
        );
    }

    /**
     * Retorna a fotografia imediatamente anterior
     * à data e hora informada.
     *
     * Será utilizada para comparação entre filas.
     */
    public Optional<LocalDateTime>
    findPreviousDateTime(
            LocalDateTime referenceDateTime
    ) {

        return findAvailableDateTimes()
                .stream()
                .filter(
                        dateTime ->
                                dateTime.isBefore(
                                        referenceDateTime
                                )
                )
                .findFirst();
    }

    /**
     * Verifica se determinado código já existe
     * em uma fotografia específica.
     */
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

    /**
     * Apaga uma fotografia específica.
     */
    @Transactional
    public void deleteByReferenceDateTime(
            LocalDateTime referenceDateTime
    ) {

        repository
                .deleteAllByReferenceDateTime(
                        referenceDateTime
                );
    }

    /**
     * Apaga todas as fotografias históricas.
     */
    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }
}