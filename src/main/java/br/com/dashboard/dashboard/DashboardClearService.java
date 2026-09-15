package br.com.dashboard.dashboard;

import br.com.dashboard.demand.DemandRepository;
import br.com.dashboard.importation.ImportHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Limpa os dados operacionais da dashboard.
 */
@Service
public class DashboardClearService {

    private final DemandRepository demandRepository;
    private final ImportHistoryRepository importHistoryRepository;

    public DashboardClearService(
            DemandRepository demandRepository,
            ImportHistoryRepository importHistoryRepository
    ) {
        this.demandRepository = demandRepository;
        this.importHistoryRepository = importHistoryRepository;
    }

    /**
     * Apaga demandas e histórico na mesma transação.
     */
    @Transactional
    public void clearAll() {
        demandRepository.deleteAll();
        importHistoryRepository.deleteAll();
    }
}