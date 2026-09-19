package br.com.dashboard.dashboard;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Disponibiliza os dados utilizados pela dashboard.
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardClearService dashboardClearService;

    public DashboardController(
            DashboardService dashboardService,
            DashboardClearService dashboardClearService
    ) {
        this.dashboardService = dashboardService;
        this.dashboardClearService = dashboardClearService;
    }

    /**
     * Retorna os indicadores principais.
     */
    @GetMapping("/summary")
    public DashboardSummary getSummary() {
        return dashboardService.getSummary();
    }

    /**
     * Retorna as demandas atrasadas mais críticas.
     */
    @GetMapping("/critical")
    public List<CriticalDemand> getCriticalDemands() {
        return dashboardService.getCriticalDemands();
    }

    /**
     * Retorna a quantidade de demandas
     * por faixa de atraso.
     */
    @GetMapping("/delay-ranges")
    public List<DelayRange> getDelayRanges() {
        return dashboardService.getDelayRanges();
    }

    /**
     * Retorna a distribuição das demandas por status.
     */
    @GetMapping("/status-distribution")
    public List<CategoryCount> getStatusDistribution() {
        return dashboardService.getStatusDistribution();
    }

    /**
     * Retorna a distribuição das demandas por setor.
     */
    @GetMapping("/sector-distribution")
    public List<CategoryCount> getSectorDistribution() {
        return dashboardService.getSectorDistribution();
    }

    /**
     * Retorna a distribuição das demandas por tipo.
     */
    @GetMapping("/type-distribution")
    public List<CategoryCount> getTypeDistribution() {
        return dashboardService.getTypeDistribution();
    }

    /**
     * Limpa somente a situação atual da dashboard
     * e o histórico técnico de importações.
     *
     * As fotografias históricas permanecem salvas.
     */
    @DeleteMapping("/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDashboard() {
        dashboardClearService.clearCurrentDashboard();
    }

    /**
     * Apaga somente as fotografias históricas.
     */
    @DeleteMapping("/clear-history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearHistory() {
        dashboardClearService.clearHistory();
    }

    /**
     * Apaga todos os dados da aplicação:
     *
     * - situação atual;
     * - histórico técnico de importações;
     * - fotografias históricas.
     */
    @DeleteMapping("/clear-everything")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearEverything() {
        dashboardClearService.clearEverything();
    }
}