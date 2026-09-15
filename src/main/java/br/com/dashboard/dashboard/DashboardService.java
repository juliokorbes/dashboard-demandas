package br.com.dashboard.dashboard;

import br.com.dashboard.demand.Demand;
import br.com.dashboard.demand.DemandService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula os indicadores da dashboard.
 */
@Service
public class DashboardService {

    private final DemandService demandService;
    private final int dueSoonDays;
    private final Set<String> completedStatuses;

    public DashboardService(
            DemandService demandService,
            @Value("${dashboard.due-soon-days:5}") int dueSoonDays,
            @Value("${dashboard.completed-statuses:CONCLUIDA}") String completedStatuses
    ) {
        this.demandService = demandService;
        this.dueSoonDays = dueSoonDays;

        this.completedStatuses = Arrays.stream(completedStatuses.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
    }

    /**
     * Calcula o resumo geral das demandas.
     */
    public DashboardSummary getSummary() {

        List<Demand> demands = demandService.findAll();

        LocalDate today = LocalDate.now();
        LocalDate dueSoonLimit = today.plusDays(dueSoonDays);

        long completed = demands.stream()
                .filter(this::isCompleted)
                .count();

        long overdue = demands.stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getDeadline() != null)
                .filter(demand -> demand.getDeadline().isBefore(today))
                .count();

        long dueSoon = demands.stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getDeadline() != null)
                .filter(demand -> !demand.getDeadline().isBefore(today))
                .filter(demand -> !demand.getDeadline().isAfter(dueSoonLimit))
                .count();

        long onTime = demands.stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getDeadline() != null)
                .filter(demand -> demand.getDeadline().isAfter(dueSoonLimit))
                .count();

        long noDeadline = demands.stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getDeadline() == null)
                .count();

        return new DashboardSummary(
                demands.size(),
                overdue,
                dueSoon,
                onTime,
                completed,
                noDeadline
        );
    }

    /**
     * Retorna as demandas atrasadas da mais crítica para a menos crítica.
     */
    public List<CriticalDemand> getCriticalDemands() {

        LocalDate today = LocalDate.now();

        return demandService.findAll()
                .stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getDeadline() != null)
                .filter(demand -> demand.getDeadline().isBefore(today))
                .map(demand -> new CriticalDemand(
                        demand.getId(),
                        demand.getExternalId(),
                        demand.getType(),
                        demand.getSector(),
                        demand.getDeadline(),
                        calculateDaysOverdue(demand, today),
                        demand.getStatus()
                ))
                .sorted((first, second) ->
                        Long.compare(
                                second.daysOverdue(),
                                first.daysOverdue()
                        )
                )
                .toList();
    }

    /**
     * Retorna a quantidade de demandas por faixa de atraso.
     */
    public List<DelayRange> getDelayRanges() {

        LocalDate today = LocalDate.now();

        List<Long> delays = demandService.findAll()
                .stream()
                .filter(demand -> !isCompleted(demand))
                .filter(demand -> demand.getDeadline() != null)
                .filter(demand -> demand.getDeadline().isBefore(today))
                .map(demand -> calculateDaysOverdue(demand, today))
                .toList();

        long oneToFive = delays.stream()
                .filter(days -> days >= 1 && days <= 5)
                .count();

        long sixToTen = delays.stream()
                .filter(days -> days >= 6 && days <= 10)
                .count();

        long elevenToThirty = delays.stream()
                .filter(days -> days >= 11 && days <= 30)
                .count();

        long overThirty = delays.stream()
                .filter(days -> days > 30)
                .count();

        return List.of(
                new DelayRange("1-5", oneToFive),
                new DelayRange("6-10", sixToTen),
                new DelayRange("11-30", elevenToThirty),
                new DelayRange("30+", overThirty)
        );
    }

    /**
     * Calcula os dias de atraso.
     */
    private long calculateDaysOverdue(Demand demand, LocalDate today) {
        return ChronoUnit.DAYS.between(
                demand.getDeadline(),
                today
        );
    }

    /**
     * Retorna a quantidade de demandas por status.
     */
    public List<CategoryCount> getStatusDistribution() {

        return demandService.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        demand -> {
                            if (demand.getStatus() == null || demand.getStatus().isBlank()) {
                                return "SEM_STATUS";
                            }

                            return demand.getStatus();
                        },
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new CategoryCount(
                        entry.getKey(),
                        entry.getValue()
                ))
                .sorted((first, second) ->
                        Long.compare(second.count(), first.count())
                )
                .toList();
    }

    /**
     * Retorna a quantidade de demandas por setor.
     */
    public List<CategoryCount> getSectorDistribution() {

        return demandService.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        demand -> {
                            if (demand.getSector() == null || demand.getSector().isBlank()) {
                                return "SEM_SETOR";
                            }

                            return demand.getSector();
                        },
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new CategoryCount(
                        entry.getKey(),
                        entry.getValue()
                ))
                .sorted((first, second) ->
                        Long.compare(second.count(), first.count())
                )
                .toList();
    }

    /**
     * Retorna a quantidade de demandas por tipo.
     */
    public List<CategoryCount> getTypeDistribution() {

        return demandService.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        demand -> {
                            if (demand.getType() == null || demand.getType().isBlank()) {
                                return "SEM_TIPO";
                            }

                            return demand.getType();
                        },
                        Collectors.counting()
                ))
                .entrySet()
                .stream()
                .map(entry -> new CategoryCount(
                        entry.getKey(),
                        entry.getValue()
                ))
                .sorted((first, second) ->
                        Long.compare(second.count(), first.count())
                )
                .toList();
    }

    /**
     * Verifica se a demanda está concluída.
     */
    private boolean isCompleted(Demand demand) {

        if (demand.getStatus() == null) {
            return false;
        }

        return completedStatuses.contains(
                demand.getStatus().trim().toUpperCase()
        );
    }
}