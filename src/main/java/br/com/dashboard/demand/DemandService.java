package br.com.dashboard.demand;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Contém as regras de negócio das demandas.
 */
@Service
public class DemandService {

    private final DemandRepository demandRepository;

    public DemandService(DemandRepository demandRepository) {
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
        return demandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demanda não encontrada"));
    }

    /**
     * Busca demandas pelo identificador externo.
     */
    public List<Demand> searchByExternalId(String externalId) {
        return demandRepository.findByExternalIdContainingIgnoreCase(externalId);
    }

    /**
     * Calcula quantos dias a demanda está atrasada.
     */
    public long calculateDaysOverdue(Long id) {
        Demand demand = findById(id);

        if (demand.getDeadline() == null) {
            return 0;
        }

        LocalDate today = LocalDate.now();

        if (!demand.getDeadline().isBefore(today)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(demand.getDeadline(), today);
    }

    /**
     * Retorna o total de demandas.
     */
    public long countAll() {
        return demandRepository.count();
    }

    /**
     * Retorna quantas demandas estão atrasadas.
     */
    public long countOverdue() {
        LocalDate today = LocalDate.now();

        return demandRepository.findAll()
                .stream()
                .filter(demand -> demand.getDeadline() != null)
                .filter(demand -> demand.getDeadline().isBefore(today))
                .count();
    }

    /**
     * Verifica se o identificador já existe.
     */
    public boolean existsByExternalId(String externalId) {
        return demandRepository.existsByExternalIdIgnoreCase(externalId);
    }

    /**
     * Salva uma demanda.
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