package br.com.dashboard.demand;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Contém as regras de negócio das demandas.
 */
@Service
public class DemandService {

    private final DemandRepository demandRepository;

    public DemandService(
            DemandRepository demandRepository
    ) {
        this.demandRepository = demandRepository;
    }

    /**
     * Retorna todas as demandas.
     */
    public List<Demand> findAll() {
        return demandRepository.findAll();
    }

    /**
     * Busca uma demanda pelo ID.
     */
    public Demand findById(Long id) {

        return demandRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException(
                                "Demanda não encontrada"
                        )
                );
    }

    /**
     * Busca demandas contendo o identificador informado.
     */
    public List<Demand> searchByExternalId(
            String externalId
    ) {

        return demandRepository
                .findByExternalIdContainingIgnoreCase(
                        externalId
                );
    }

    /**
     * Busca exatamente uma demanda pelo código do Asgard.
     */
    public Optional<Demand> findByExternalId(
            String externalId
    ) {

        if (
                externalId == null ||
                        externalId.isBlank()
        ) {
            return Optional.empty();
        }

        return demandRepository
                .findByExternalIdIgnoreCase(
                        externalId.trim()
                );
    }

    /**
     * Calcula quantos dias a demanda está atrasada.
     *
     * Esta regra ainda utiliza vencimento.
     * Posteriormente a dashboard será adaptada
     * para utilizar QUALIFICAÇÃO como prioridade.
     */
    public long calculateDaysOverdue(Long id) {

        Demand demand =
                findById(id);

        if (demand.getDeadline() == null) {
            return 0;
        }

        LocalDate today =
                LocalDate.now();

        if (
                !demand.getDeadline()
                        .isBefore(today)
        ) {
            return 0;
        }

        return ChronoUnit.DAYS.between(
                demand.getDeadline(),
                today
        );
    }

    /**
     * Retorna o total de demandas.
     */
    public long countAll() {
        return demandRepository.count();
    }

    /**
     * Retorna quantas demandas estão atrasadas.
     *
     * Esta regra ainda utiliza vencimento e será
     * atualizada junto com a nova dashboard.
     */
    public long countOverdue() {

        LocalDate today =
                LocalDate.now();

        return demandRepository
                .findAll()
                .stream()
                .filter(
                        demand ->
                                demand.getDeadline()
                                        != null
                )
                .filter(
                        demand ->
                                demand.getDeadline()
                                        .isBefore(today)
                )
                .count();
    }

    /**
     * Verifica se o identificador já existe.
     */
    public boolean existsByExternalId(
            String externalId
    ) {

        return demandRepository
                .existsByExternalIdIgnoreCase(
                        externalId
                );
    }

    /**
     * Salva ou atualiza uma demanda.
     */
    public Demand save(Demand demand) {
        return demandRepository.save(demand);
    }

    /**
     * Apaga todas as demandas armazenadas.
     */
    public void deleteAll() {
        demandRepository.deleteAll();
    }
}