package br.com.dashboard.dashboard;

import br.com.dashboard.demand.DemandRepository;
import br.com.dashboard.history.DemandSnapshotRepository;
import br.com.dashboard.importation.ImportHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsável pela limpeza dos dados da aplicação.
 */
@Service
public class DashboardClearService {

    private final DemandRepository demandRepository;
    private final ImportHistoryRepository importHistoryRepository;
    private final DemandSnapshotRepository demandSnapshotRepository;

    public DashboardClearService(
            DemandRepository demandRepository,
            ImportHistoryRepository importHistoryRepository,
            DemandSnapshotRepository demandSnapshotRepository
    ) {
        this.demandRepository = demandRepository;
        this.importHistoryRepository = importHistoryRepository;
        this.demandSnapshotRepository = demandSnapshotRepository;
    }

    /**
     * Limpa somente a situação atual da dashboard
     * e o histórico técnico de importações.
     *
     * As fotografias históricas permanecem salvas.
     */
    @Transactional
    public void clearCurrentDashboard() {
        demandRepository.deleteAll();
        importHistoryRepository.deleteAll();
    }

    /**
     * Mantido por compatibilidade com o controller atual.
     *
     * O botão "Limpar dashboard" continua limpando
     * somente a situação atual.
     */
    @Transactional
    public void clearAll() {
        clearCurrentDashboard();
    }

    /**
     * Apaga somente as fotografias históricas.
     */
    @Transactional
    public void clearHistory() {
        demandSnapshotRepository.deleteAll();
    }

    /**
     * Apaga absolutamente tudo:
     *
     * - situação atual;
     * - histórico de importações;
     * - fotografias históricas.
     *
     * Deve ser usado somente em uma ação
     * específica e com confirmação forte.
     */
    @Transactional
    public void clearEverything() {
        demandSnapshotRepository.deleteAll();
        demandRepository.deleteAll();
        importHistoryRepository.deleteAll();
    }
}