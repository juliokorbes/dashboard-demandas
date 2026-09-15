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
     * Limpa as demandas e o histórico de importações.
     */
    @DeleteMapping("/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDashboard() {
        dashboardClearService.clearAll();
    }
}