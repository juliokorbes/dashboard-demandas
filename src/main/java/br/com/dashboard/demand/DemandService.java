package br.com.dashboard.demand;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Contém as regras de negócio das demandas.
 *
 * A tabela de demandas representa somente
 * a situação atual da dashboard.
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
     * Retorna todas as demandas da situação atual.
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
     * Substitui completamente a situação atual.
     *
     * A lista recebida representa a fotografia
     * mais recente das filas monitoradas.
     *
     * Registros que existiam anteriormente e não
     * aparecem na nova fotografia deixam de fazer
     * parte da situação atual.
     *
     * Eles NÃO são apagados do histórico de snapshots.
     */
    @Transactional
    public void replaceCurrentSituation(
            List<Demand> demands
    ) {

        demandRepository.deleteAll();
        demandRepository.flush();

        for (Demand demand : demands) {

            /*
             * Como estamos criando uma nova representação
             * da situação atual, garantimos que o JPA
             * trate os objetos como novos registros.
             */
            demand.setId(null);

            demandRepository.save(
                    demand
            );
        }
    }

    /**
     * Calcula quantos dias a demanda está atrasada
     * utilizando o vencimento.
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
     * Retorna o total de demandas atuais.
     */
    public long countAll() {
        return demandRepository.count();
    }

    /**
     * Retorna quantas demandas estão atrasadas
     * utilizando o vencimento.
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
     * Verifica se o identificador existe
     * na situação atual.
     */
    public boolean existsByExternalId(
            String externalId
    ) {

        if (
                externalId == null ||
                        externalId.isBlank()
        ) {
            return false;
        }

        return demandRepository
                .existsByExternalIdIgnoreCase(
                        externalId.trim()
                );
    }

    /**
     * Salva ou atualiza uma demanda.
     */
    public Demand save(Demand demand) {
        return demandRepository.save(demand);
    }

    /**
     * Apaga todas as demandas da situação atual.
     *
     * Não interfere no histórico de snapshots.
     */
    @Transactional
    public void deleteAll() {
        demandRepository.deleteAll();
    }
}